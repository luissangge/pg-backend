package uy.com.pepeganga.productsservice.services;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uy.com.pepeganga.productsservice.clients.IStoreServiceClient;
import uy.com.pepeganga.productsservice.entities.Item;
import uy.com.pepeganga.productsservice.gridmodels.ItemGrid;
import uy.com.pepeganga.productsservice.repository.ProductsRepository;

@Service
public class ItemService {

	@Autowired
	IStoreServiceClient client;
	
	@Autowired
	ProductsRepository productsRepository;
	
	public List<ItemGrid> getItemsListGrid()
	{
		return client.getItemsGridTemporally();
	}
	
	public Page<Item> findAll(String sku, String nameProduct, String categoryNameDescription, double minPrice, double maxPrice, Pageable pageable) {
		return  productsRepository.findAll((Root<Item> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
					List<Predicate> predicates = new ArrayList<>();
					predicates.add(cb.between(root.get("precioPesos"), minPrice, maxPrice));
					if(!sku.equals("")) {
						predicates.add(cb.like(root.get("sku").as(String.class), "%"+ sku + "%"));
					}
					if(!categoryNameDescription.equals("")) {
						predicates.add(cb.like(root.get("artDescripCatalogo").as(String.class), "%"+ nameProduct + "%"));
					}
					if(!categoryNameDescription.equals("")){
						predicates.add(cb.like(root.join("categories").get("description").as(String.class),"%"+ categoryNameDescription + "%"));
					}
					return query.where(predicates.toArray(new Predicate[predicates.size()])).getRestriction();
				}, pageable);
 
	}
}
