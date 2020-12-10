package uy.com.pepeganga.consumingwsstore.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uy.com.pepeganga.business.common.entities.*;
import uy.com.pepeganga.business.common.utils.date.DateTimeUtilsBss;
import uy.com.pepeganga.consumingwsstore.client.MeliFeignClient;
import uy.com.pepeganga.consumingwsstore.entities.*;
import uy.com.pepeganga.consumingwsstore.models.Pair;
import uy.com.pepeganga.consumingwsstore.models.RiskTime;
import uy.com.pepeganga.consumingwsstore.repositories.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@EnableAsync
@Service
public class ScheduledSyncService implements IScheduledSyncService{

    private static final Logger logger = LoggerFactory.getLogger(ScheduledSyncService.class);
    static RestTemplate restTemplate = new RestTemplate();
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    MeliFeignClient feign;
    @Autowired
    RiskTime property;

    @Autowired
    ProductsRepository productRepo;

    //Repositories of temporal Tables
  /*  @Autowired
    ITempBrandRepository tempBrandRepo;
    @Autowired
    ITempCategoryRepository tempCategoryRepo;
    @Autowired
    ITempFamilyRepository tempFamilyRepo;
    @Autowired
    ITempItemRepository tempItemRepo;
 */   @Autowired
    IUpdatesSystemRepository updateSysRepo;

    //Repositories of Tables
    @Autowired
    IItemRepository itemRepo;
    @Autowired
    IFamilyRepository familyRepo;
    @Autowired
    ICategoryRepository categoryRepo;
    @Autowired
    IBrandRepository brandRepo;
    @Autowired
    CheckingStockProcessorRepository checkStockRepo;
    @Autowired
    StockProcessorRepository stockProcRepo;

    //Services
    @Autowired
    BrandRequestService brandService;
    @Autowired
    CategoryRequestService categoryService;
    @Autowired
    FamilyRequestService familyService;
    @Autowired
    ItemRequestService itemService;

    UpdatesOfSystem data = new UpdatesOfSystem();

    @Override
    public void syncDataBase(){

        try {
            //Create synchronization logs
            logger.info("Starting synchronization");
            UpdatesOfSystem updatesSystem = new UpdatesOfSystem();
            updatesSystem.setStartDate(DateTimeUtilsBss.getDateTimeAtCurrentTime().toDate());
            data = updateSysRepo.save(updatesSystem);

            //Insert synchronization data
            String[] dataTypes = new String[]{
                    "brand","family","category","item" };

            for(int i = 0; i < dataTypes.length; i++) {
                if(!insertData(dataTypes[i])) {
                    return;
                }
            }

            //Update Stock table
            if(!updateStockProvided()){
                logger.error(String.format("Error updating stock to publications in Mercado Libre, Error: "));
                return;
            }
/*
            //If this is execute then the synchronization was Ok, for that reason Empty temporals table
            deleteTemporalData();
*/

        }catch (Exception e) {
            logger.error(String.format("Error synchronizing Tables {General method}, Error: "), e.getMessage());
            updateTableLogs(String.format("Error synchronizing Tables {General method}, Error: %s", e.getMessage()), true);
            return;
        }

    }

    private boolean insertData(String type) {
        logger.info(String.format("Starting to insert or update %s data in table", type));
        try {
            if (type.equals("brand"))
                if(!brandService.storeBrand(data))
                    return false;
            if (type.equals("family"))
                if(!familyService.storeFamilies(data))
                    return false;
            if (type.equals("category"))
                if(!categoryService.storeCategories(data))
                    return false;
            if (type.equals("item"))
                if(!itemService.storeItems(data))
                    return false;
            logger.info(String.format("Insert or Update %s data in table Completed....", type));
            return true;
        }catch (Exception e){
            logger.error(String.format("Error inserting or updating %s tables {}", type), e.getMessage());
            updateTableLogs(String.format("Error inseting or updating %s tables {}", type) + e.getMessage(), true);
            //deleteTemporalData();
            return false;
        }
    }

    private void updateTableLogs(String msg, Boolean isError) {
        if(isError) {
            data.setFinishedSync(false);
        }
        else {
            data.setFinishedSync(true);
        }
        String data1 = data.getMessage();
        data.setMessage(data1 + msg);
        data.setEndDate(DateTimeUtilsBss.getDateTimeAtCurrentTime().toDate());
        updateSysRepo.save(data);
    }

    private boolean updateStockProvided(){
        logger.info("Starting to update stock provider...");
        List<StockProcessor> stockList = new ArrayList<>();
        stockList.addAll(stockProcRepo.findAll());
        List<Item> itemsU = new ArrayList<>();
        itemsU = itemRepo.findAll();
        List<String> itemsToUpdate = new ArrayList<>();

        List<Pair> pairs = new ArrayList<>();
        boolean initialStockEmpty = false;
        boolean finishedWithError = false;

        List<CheckingStockProcessor> checkingList = new ArrayList<>();
        List<StockProcessor> stockToAddOrUpdate = new ArrayList<>();

        if(!stockList.isEmpty()){
            itemsU.forEach(newItem -> {
                pairs.add(new Pair(newItem.getSku(), Math.toIntExact(newItem.getStockActual())));
                boolean exit = false;
                AtomicInteger count = new AtomicInteger();
                CheckingStockProcessor checking = new CheckingStockProcessor();
                StockProcessor stockUpdated = new StockProcessor();

                while (count.get() < stockList.size() && !exit){
                    //Si existe el SKU en la tabla?
                    if(stockList.get(count.get()).getSku().equals(newItem.getSku())){
                        exit = true;
                        stockUpdated.setId(stockList.get(count.get()).getId());
                        stockUpdated.setSku(stockList.get(count.get()).getSku());
                        stockUpdated.setExpectedStock(stockList.get(count.get()).getExpectedStock());
                        stockUpdated.setRealStock(Math.toIntExact(newItem.getStockActual()));

                        //el articulo esta pausado por stock
                        if((stockList.get(count.get()).getRealStock() - stockList.get(count.get()).getExpectedStock()) <= property.getRiskTime()){
                            //Verifico si continua sin stock al actualizar
                            if((int) newItem.getStockActual() - stockList.get(count.get()).getExpectedStock() > property.getRiskTime()){
                                checking.setSku(stockList.get(count.get()).getSku());
                                checking.setExpectedStock(stockList.get(count.get()).getExpectedStock());
                                checking.setRealStock((int) newItem.getStockActual());
                                checking.setAction(0);
                                checkingList.add(checking);
                                logger.info("Enviando al checking con item con sku: {}", checking.getSku());
                            }
                        }
                        //Articulo no pausado por stock -- Verifico si se pausa al actualizar
                        else{
                            if((int) newItem.getStockActual() - stockList.get(count.get()).getExpectedStock() <= property.getRiskTime()){
                                checking.setSku(stockList.get(count.get()).getSku());
                                checking.setExpectedStock(stockList.get(count.get()).getExpectedStock());
                                checking.setRealStock((int) newItem.getStockActual());
                                checking.setAction(0);
                                checkingList.add(checking);
                                logger.info("Enviando al checking con item con sku: {}", checking.getSku());
                            }
                        }
                    }
                    count.getAndIncrement();
                }
                if(!exit) {
                    //No existe el articulo con SKU en la tabla StockProcessor -- lo adiciono a StockProcessor Table
                    logger.info("Adicionando sku a la tabla Stock Processor: {}", checking.getSku());
                    stockUpdated.setSku(newItem.getSku());
                    stockUpdated.setExpectedStock(0);
                    stockUpdated.setRealStock((int) newItem.getStockActual());

                    //Verifico si cumple condicion de stock
                    if ((int) newItem.getStockActual() - 0 <= property.getRiskTime()) {
                        checking.setSku(newItem.getSku());
                        checking.setExpectedStock(0);
                        checking.setRealStock((int) newItem.getStockActual());
                        checking.setAction(0);
                        checkingList.add(checking);
                        logger.info("Enviando al checking con item con sku: {}", checking.getSku());
                    }
                }

                //si existe articulo: Actualizo el stock del articulo en la tabla Stock Processor, sino existe: lo adiciono
                stockToAddOrUpdate.add(stockUpdated);
            });
        }
        //El stock está vacio -- Sistema nuevo
        else{
            itemsU.forEach(i -> {
                StockProcessor stockNew = new StockProcessor();
                CheckingStockProcessor checkingNew = new CheckingStockProcessor();
                //adiciono a StockProcessor Table
                logger.info("Adicionando sku a la tabla Stock Processor: {}", i.getSku());
                stockNew.setSku(i.getSku());
                stockNew.setExpectedStock(0);
                stockNew.setRealStock((int) i.getStockActual());

                //Verifico si cumple condicion de stock
                if ((int) i.getStockActual() - 0 <= property.getRiskTime()) {
                    checkingNew.setSku(i.getSku());
                    checkingNew.setExpectedStock(0);
                    checkingNew.setRealStock((int) i.getStockActual());
                    checkingNew.setAction(0);
                    checkingList.add(checkingNew);
                    logger.info("Enviando al checking con item con sku: {}", checkingNew.getSku());
                }
                stockToAddOrUpdate.add(stockNew);
            });
            initialStockEmpty = true;
        }

        //Productos a pausar especialmente por no existir
        List<Item> finalItemsU1 = new ArrayList<>();
        List<StockProcessor> deleteStockList = new ArrayList<>();
        finalItemsU1.addAll(itemsU);

        if(!initialStockEmpty)
            stockList.forEach(s -> {
            boolean exit1 = false;
            boolean specialPause = true;
            int j = 0;
            while (j < finalItemsU1.size() && !exit1){
                if(finalItemsU1.get(j).getSku().equals(s.getSku())){
                    specialPause = false;
                    exit1 = true;
                }
                j++;
            }
            if(exit1) {
                j--;
                finalItemsU1.remove(j);
            }
            if(specialPause) {
                //Pausamos este Item por no llegar en la actualización
                CheckingStockProcessor check = new CheckingStockProcessor();
                check.setSku(s.getSku());
                check.setExpectedStock(0);
                check.setRealStock(0);
                check.setAction(0);
                checkingList.add(check);

                //Actualizamos el stock del Producto a cero en la tabla Item
                j--;
                itemsToUpdate.add(finalItemsU1.get(j).getSku());
                pairs.add(new Pair(finalItemsU1.get(j).getSku(), 0));

                //actualizamos a cero el item en la tabla Stock Processor
                s.setRealStock(0);
                s.setExpectedStock(0);
                stockToAddOrUpdate.add(s);
                logger.info("Enviando al checking para pausar especial el item con sku: {} por no venir en la actualización", s.getSku());
            }
        });
        //Liberando espacio en memoria
        finalItemsU1.clear();

        logger.info("Updating checking Stock and stock product table");
        //Actualizar ambas tablas en la base datos
        if(!checkingList.isEmpty()) {
            checkingList.forEach(check -> {
                CheckingStockProcessor data = checkStockRepo.findBySku(check.getSku());
                if(data != null)
                    check.setId(data.getId());
            });
            checkStockRepo.saveAll(checkingList);
        }

        if(!stockToAddOrUpdate.isEmpty())
            stockProcRepo.saveAll(stockToAddOrUpdate);
        logger.info("Checking Stock and stock product table completed...");

        List<Item> updateItem = new ArrayList<>();
        itemsToUpdate.forEach(i -> {
            Optional<Item> itemOptional = itemRepo.findById(i);
            if(itemOptional.isPresent()) {
                itemOptional.get().setStockActual(0);
                updateItem.add(itemOptional.get());
            }
        });
        if(!updateItem.isEmpty())
            itemRepo.saveAll(updateItem);

        logger.info("Starting update publications in Mercado Libre");

        //Actualiza stock en el sistema y en Mercado Libre
        if(!initialStockEmpty) {
            if(!updateStockOfProductsMeli(itemsU)){
               finishedWithError = true;
            }
            updateStockOfPublicationsMeli(pairs);

        }
        logger.info("Update publications in Mercado Libre Completed...");

        if(finishedWithError){ return false;}
        else {
            if(data.getEndDate() == null) {
                updateTableLogs("Synchronization Completed...", false);
                return true;
            }
            else {
                return false;
            }
        }
    }

    private boolean updateStockOfProductsMeli(List<Item> itemsU){
        try {
            itemsU.forEach( i -> {
                productRepo.updateStockBySKU(i.getStockActual(), i.getSku());
            });
            return true;
        }catch (Exception e){
            logger.error(String.format("Error updating stock in mercadolibrepublications table {} Error: "), e.getMessage());
            updateTableLogs(String.format("Error updating stock in mercadolibrepublications table {} Error: ", e.getMessage()), true);
            return false;
        }

    }

    @Async
    public boolean updateStockOfPublicationsMeli(List<Pair> pairs){
        try {
            boolean p1 = feign.updateStock(pairs, data.getId());
            return true;
        }catch (Exception e) {
            logger.error(String.format("Error calling meli service to update stock of publications {} Error: "), e.getMessage());
            updateTableLogs(String.format("Error calling meli service to update stock of publications {} Error: ", e.getMessage()), true);
            return false;
        }
    }

    private Item addItem(TempItem ti) {
        Item item = new Item();
        item.setSku(ti.getSku());
        item.setArtCantUnidades(ti.getArtCantUnidades());
        item.setArtCodPro(ti.getArtCodPro());
        item.setArtDescripCatalogo(ti.getArtDescripCatalogo());
        item.setArtDescripML(ti.getArtDescripML());
        item.setArtEspecificaciones(ti.getArtEspecificaciones());
        item.setArtMedida(ti.getArtMedida());
        item.setArtMostrarEnWeb(ti.getArtMostrarEnWeb());
        item.setArtNombreML(ti.getArtNombreML());
        item.setArtPreUniDolares(ti.getArtPreUniDolares());
        item.setArtPreUniPesos(ti.getArtPreUniPesos());
        item.setArtVendibleMercadoLibre(ti.getArtVendibleMercadoLibre());
        item.setCantidadPorCaja(ti.getCantidadPorCaja());
        item.setCapacidad(ti.getCapacidad());
        item.setFuturo(ti.getFuturo());
        item.setMedidaEmpaque(ti.getMedidaEmpaque());
        item.setImages(ti.getImages());
        item.setNuevo(ti.getNuevo());
        item.setOferta(ti.getOferta());
        item.setPrecioDolares(ti.getPrecioDolares());
        item.setPrecioPesos(ti.getPrecioPesos());
        item.setStockActual(ti.getStockActual());
        item.setTalle(ti.getTalle());
        List<Category> catList = new ArrayList<>();
        ti.getCategories().forEach(cat ->
        {
            Category category = new Category();
            category.setId(cat.getId());
            category.setDescription(cat.getDescription());
            catList.add(category);
        });
        if(catList.size() != 0)
            item.setCategories(catList);

        if(ti.getFamily() != null){
            Family family = new Family();
            family.setId(ti.getFamily().getId());
            family.setDescription(ti.getFamily().getDescription());
            item.setFamily(family);
        }

        if(ti.getBrand() != null) {
            Brand brand = new Brand();
            brand.setId(ti.getBrand().getId());
            brand.setDescription(ti.getBrand().getDescription());
            brand.setMarcaInUse(ti.getBrand().getMarcaInUse());
            item.setBrand(brand);
        }

        return item;
    }
    private Item updateItem(TempItem ti, Item item) {
        item.setArtCantUnidades(ti.getArtCantUnidades());
        item.setArtCodPro(ti.getArtCodPro());
        item.setArtDescripCatalogo(ti.getArtDescripCatalogo());
        item.setArtDescripML(ti.getArtDescripML());
        item.setArtEspecificaciones(ti.getArtEspecificaciones());
        item.setArtMedida(ti.getArtMedida());
        item.setArtMostrarEnWeb(ti.getArtMostrarEnWeb());
        item.setArtNombreML(ti.getArtNombreML());
        item.setArtPreUniDolares(ti.getArtPreUniDolares());
        item.setArtPreUniPesos(ti.getArtPreUniPesos());
        item.setArtVendibleMercadoLibre(ti.getArtVendibleMercadoLibre());
        item.setCantidadPorCaja(ti.getCantidadPorCaja());
        item.setCapacidad(ti.getCapacidad());
        item.setFuturo(ti.getFuturo());
        item.setMedidaEmpaque(ti.getMedidaEmpaque());
        item.setImages(ti.getImages());
        item.setNuevo(ti.getNuevo());
        item.setOferta(ti.getOferta());
        item.setPrecioDolares(ti.getPrecioDolares());
        item.setPrecioPesos(ti.getPrecioPesos());
        item.setStockActual(ti.getStockActual());
        item.setTalle(ti.getTalle());
        return item;
    }
    private Category addCategory(TempCategory cat) {
        Category category = new Category();
        category.setId(cat.getId());
        category.setDescription(cat.getDescription());
        return category;
    }
    private Brand addBrand(TempBrand b) {
        Brand brand = new Brand();
        brand.setId(b.getId());
        brand.setDescription(b.getDescription());
        brand.setMarcaInUse(b.getMarcaInUse());
        return brand;
    }
    private Family addFamily(TempFamily fami) {
        Family family = new Family();
        family.setId(fami.getId());
        family.setDescription(fami.getDescription());
        return family;
    }

    private boolean saveOrDeleteItemsTable(List<Item> list, String action){
        logger.info("Starting to synchronization from TempItems to Item table in database");
        try {
            if(action.equals("deleting")) {
                if(list.size() != 0)
                    itemRepo.deleteAll(list);
            }else if(action.equals("saving")){
                itemRepo.saveAll(list);
            }
            logger.info("The synchronization from TempItems to Item table in database completed...");
            return true;
        }catch (Exception e){
            logger.error("The synchronization from TempItems to Item table in database throw a error");
            logger.error(String.format("Error %s Item table {}", action), e.getMessage());
            updateTableLogs(String.format("Error %s Item table {}", action) + e.getMessage(), true);
            return false;
        }
    }
    private boolean saveOrDeleteBrandsTable(List<Brand> list, String action){
        logger.info("Starting to synchronization from TempBrand to Brand table in database");
        try {
            if(action.equals("deleting")) {
                if(list.size() != 0)
                    brandRepo.deleteAll(list);
            }else if(action.equals("saving")){
                brandRepo.saveAll(list);
            }
            logger.info("The synchronization from TempBrand to Brand table in database completed...");
            return true;
        }catch (Exception e){
            logger.error("The synchronization from TempBrand to Brand table in database throw a error");
            logger.error(String.format("Error %s Brand table {}", action), e.getMessage());
            updateTableLogs(String.format("Error %s Brand table {}", action) + e.getMessage(), true);
            return false;
        }
    }
    private boolean saveOrDeleteFamiliesTable(List<Family> list, String action){
        logger.info("Starting to synchronization from TempFamilies to Families table in database");
        try {
            if(action.equals("deleting")) {
                if(list.size() != 0)
                    familyRepo.deleteAll(list);
            }else if(action.equals("saving")){
                familyRepo.saveAll(list);
            }
            logger.info("The synchronization from TempFamilies to Families table in database completed...");
            return true;
        }catch (Exception e){
            logger.error("The synchronization from TempFamilies to Families table in database throw a error");
            logger.error(String.format("Error %s Family table {}", action), e.getMessage());
            updateTableLogs(String.format("Error %s Family table {}", action) + e.getMessage(), true);
            return false;
        }
    }
    private boolean saveOrDeleteCategoriesTable(List<Category> list, String action){
        logger.info("Starting to synchronization from TempCategories to Categories table in database");
        try {
            if(action.equals("deleting")) {
                if(list.size() != 0)
                    categoryRepo.deleteAll(list);
            }else if(action.equals("saving")){
                categoryRepo.saveAll(list);
            }
            logger.info("The synchronization from TempCategories to Categories table in database completed...");
            return true;
        }catch (Exception e){
            logger.error("The synchronization from TempCategories to Categories table in database throw a error");
            logger.error(String.format("Error %s Category table {}", action), e.getMessage());
            updateTableLogs(String.format("Error %s Category table {}", action) + e.getMessage(), true);
            return false;
        }
    }
}