package uy.com.pepeganga.consumingwsstore.conversions;
import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import uy.com.pepeganga.business.common.entities.*;
import uy.com.pepeganga.consumingwsstore.gridmodels.ItemGrid;
import uy.com.pepeganga.consumingwsstore.wsdl.families.SdtLineasSubFliasSdtLineaSubFlias;
import uy.com.pepeganga.consumingwsstore.wsdl.families.SdtSubFliasSdtSubFlia;
import uy.com.pepeganga.consumingwsstore.wsdl.items.SDTArticulosWebPaginaArticulo;
import uy.com.pepeganga.consumingwsstore.wsdl.items.SdtArtFotosSdtArtFoto;
import uy.com.pepeganga.consumingwsstore.wsdl.items.SdtCategoriasSdtCategoria;
import uy.com.pepeganga.consumingwsstore.wsdl.marcas.SdtMarcasSdtMarca;

public class ConvertModels {

	@Autowired
	private static Item itemResp;	
	
	@Autowired
	private static Category categoryResp;	
	
	@Autowired
	private static Family familyResp;
	
	@Autowired
	private static SubFamily subfamilyResp;	
	
	@Autowired
	private static Brand brandResp;
	
	@Autowired
	private static ItemGrid itemGridResp;
	
	
	public static Item convetToItemEntity(SDTArticulosWebPaginaArticulo artResp) {
		itemResp = new Item();		
		
		if (!isNull(artResp))			
		 {				
			itemResp.setSku(artResp.getId());
			itemResp.setArtDescripCatalogo(artResp.getArtDescripCatalogo());
			itemResp.setArtMedida(artResp.getArtMedida());
			itemResp.setArtEspecificaciones(artResp.getArtEspecificaciones());			
			itemResp.setCantidadPorCaja(artResp.getCantidadPorCaja());
			itemResp.setPrecioPesos(artResp.getPrecioPesos());
			itemResp.setPrecioDolares(artResp.getPrecioDolares());
			itemResp.setFuturo(artResp.getFuturo());
			itemResp.setNuevo(artResp.getNuevo());
			itemResp.setOferta(artResp.getOferta());
			itemResp.setStockActual(artResp.getStockActual());
			itemResp.setArtCantUnidades(artResp.getArtCantUnidades());
			itemResp.setArtPreUniPesos(artResp.getArtPreUniPesos());
			itemResp.setArtPreUniDolares(artResp.getArtPreUniDolares());		
			itemResp.setArtMostrarEnWeb(artResp.getArtMostrarEnWeb());
			itemResp.setArtVendibleMercadoLibre(artResp.getArtVendibleMercadoLibre());
			itemResp.setArtCodPro(artResp.getArtCodPro());
			itemResp.setArtNombreML(artResp.getArtNombreML());
			itemResp.setArtDescripML(artResp.getArtDescripML());
			itemResp.setMedidaEmpaque(artResp.getMedidaEmpaque());
			itemResp.setCapacidad(artResp.getCapacidad());
			itemResp.setTalle(artResp.getTalle());			
			
			if(artResp.getFamiliaId() != 0 )
				itemResp.setFamily(createFamilyInstance(artResp.getFamiliaId()));	
			if(artResp.getMarcaId() != 0)
				itemResp.setBrand(createBrandInstance(artResp.getMarcaId()));			
			
			if(artResp.getSdtCategorias() != null)
			{
				List<Category> categoryTempList = new ArrayList<>();
				for (SdtCategoriasSdtCategoria variable : artResp.getSdtCategorias().getSdtCategoriasSdtCategoria()) {
					if(variable != null)				
						categoryTempList.add(convetToCategoryEntityItem(variable));
				}		
				itemResp.setCategories(categoryTempList);
			}
				
			if(artResp.getSdtArtFotos() != null)
			{
				
				StringBuilder photo = new StringBuilder();
				for (SdtArtFotosSdtArtFoto variable : artResp.getSdtArtFotos().getSdtArtFotosSdtArtFoto()) {
					if(variable != null)
					{
						photo.append(variable.getFoto().trim()).append(" ");
					}
				}
				itemResp.setImages(photo.toString().getBytes());
			}
		}
		return itemResp;
	}

	public static List<Item> convetToItemEntityList(List<SDTArticulosWebPaginaArticulo> artList)
	{
		List<Item> itemList = new ArrayList<>();
		
		if(!artList.isEmpty())
		{
			for (SDTArticulosWebPaginaArticulo variable : artList) {
				if(variable != null)
					itemList.add(convetToItemEntity(variable));
			}
		}
		return itemList;
	}
	
	public static ItemGrid convetToItemGrid(SDTArticulosWebPaginaArticulo artResp) {
		itemGridResp = new ItemGrid();		
		
		if (!isNull(artResp))			
		 {			
			itemGridResp.setSku(artResp.getId());
			itemGridResp.setPriceUYU(artResp.getPrecioPesos());
			itemGridResp.setPriceUSD(artResp.getPrecioDolares());			
			itemGridResp.setName(artResp.getArtDescripCatalogo());
			itemGridResp.setImage(artResp.getSdtArtFotos().getSdtArtFotosSdtArtFoto().get(0).getFoto());		

			for (SdtCategoriasSdtCategoria variable : artResp.getSdtCategorias().getSdtCategoriasSdtCategoria())
				itemGridResp.getCategories().put(Short.toString(variable.getCategoriaId()),
						variable.getCategoriaDescrip());			
		}
		return itemGridResp;
	}
	
	public static List<ItemGrid> convetToItemGridList(List<SDTArticulosWebPaginaArticulo> artList)
	{
		List<ItemGrid> itemGridList = new ArrayList<>();
		
		if(!artList.isEmpty())
		{
			for (SDTArticulosWebPaginaArticulo variable : artList)
				itemGridList.add(convetToItemGrid(variable));
		}
		return itemGridList;
	}
		
	public static Category convetToCategoryEntity(uy.com.pepeganga.consumingwsstore.wsdl.categories.SdtCategoriasSdtCategoria category) {
		
		categoryResp = new Category();		
		
		if (!isNull(category))			
		 {			
			categoryResp.setId(category.getCategoriaId());
			categoryResp.setDescription(category.getCategoriaDescrip());
		 }
		 return categoryResp;
	}
	
	public static Category convetToCategoryEntityItem(SdtCategoriasSdtCategoria category) {
		
		categoryResp = new Category();		
		
		if (!isNull(category))			
		 {			
			categoryResp.setId(category.getCategoriaId());
			categoryResp.setDescription(category.getCategoriaDescrip());
		 }
		 return categoryResp;
	}
	
	public static List<Category> convetToCategoryEntityList(List<uy.com.pepeganga.consumingwsstore.wsdl.categories.SdtCategoriasSdtCategoria> catList){
		
		List<Category> categoryList = new ArrayList<>();
		
		if(!catList.isEmpty())
		{
			for (uy.com.pepeganga.consumingwsstore.wsdl.categories.SdtCategoriasSdtCategoria variable : catList)
				if(variable != null)
					categoryList.add(convetToCategoryEntity(variable));
		}
		return categoryList;
	}
	
	public static Family convetToFamilyEntity(SdtLineasSubFliasSdtLineaSubFlias family) {
		
		familyResp = new Family();		
		
		if (!isNull(family))			
		 {			
			familyResp.setId(family.getLinId());
			familyResp.setDescription(family.getLinDsc());
			if(family.getSdtSubFlias() != null)
				familyResp.setSubfamilies(convertToSubFamilyEntityList(family.getSdtSubFlias().getSdtSubFliasSdtSubFlia()));
		}
		return familyResp;
	}
	
	public static List<Family> convetToFamilyEntityList(List<SdtLineasSubFliasSdtLineaSubFlias> famiList){
		
		List<Family> familyList = new ArrayList<>();
		
		if(!famiList.isEmpty())
		{
			for (SdtLineasSubFliasSdtLineaSubFlias variable : famiList)
				if(variable != null)
					familyList.add(convetToFamilyEntity(variable));
		}
		return familyList;
	}	
	
	public static SubFamily convetToSubFamilyEntity(SdtSubFliasSdtSubFlia subfamily) {
		
		subfamilyResp = new SubFamily();		
		
		if (!isNull(subfamily))			
		 {		
			subfamilyResp.setId(subfamily.getSubFliaId());
			subfamilyResp.setDescription(subfamily.getSubFliaDsc());
		 }
		return subfamilyResp;
	}
	
	public static List<SubFamily> convertToSubFamilyEntityList(List<SdtSubFliasSdtSubFlia> subfamiList){
		
		List<SubFamily> familyList = new ArrayList<>();
		
		if(!subfamiList.isEmpty())
		{
			for (SdtSubFliasSdtSubFlia variable : subfamiList)
				if(variable != null)
					familyList.add(convetToSubFamilyEntity(variable));
		}
		return familyList;
	}
	
	public static Brand convertToBrandEntity(SdtMarcasSdtMarca brand) {
	
		brandResp = new Brand();		
		
		if (!isNull(brand))			
		 {			
			brandResp.setId(brand.getMarcaId());
			brandResp.setDescription(brand.getMarcaDescrip());
			brandResp.setMarcaInUse(brand.getMarcaEnUso());
		 }
		 return brandResp;
	}
	
	public static List<Brand> convetToBrandEntityList(List<SdtMarcasSdtMarca> brandList){
		
		List<Brand> responseList = new ArrayList<>();
		
		if(!brandList.isEmpty())
		{
			for (SdtMarcasSdtMarca variable : brandList)
				if(variable != null)
					responseList.add(convertToBrandEntity(variable));
		}
		return responseList;
	}
	
	/*Auxiliar Class*/
	//Nota: cambiar todos los metodos despues a private cuando quite la generacion ficticia de Item en el Run()
 	private static Family createFamilyInstance(short id) {
		Family family = new Family();
		family.setId(id);
		return family;
	}
	
	private static Brand createBrandInstance(short id) {
		Brand brand = new Brand();
		brand.setId(id);
		return brand;
	}
	
}
