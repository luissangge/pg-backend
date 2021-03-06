package uy.pepeganga.meli.service.models;

import meli.model.Item;
import uy.com.pepeganga.business.common.entities.ImagePublicationMeli;

import java.util.List;

public class ItemModel {
    private Item item;
    private Integer idPublicationProduct;
    private String sku;
    private List<ImagePublicationMeli> images;
    private double priceCostUYU;
    private double priceCostUSD;
    private double priceEditProduct;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Integer getIdPublicationProduct() {
        return idPublicationProduct;
    }

    public void setIdPublicationProduct(Integer idPublicationProduct) {
        this.idPublicationProduct = idPublicationProduct;
    }
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
    public List<ImagePublicationMeli> getImages() {
        return images;
    }

    public void setImages(List<ImagePublicationMeli> images) {
        this.images = images;
    }

    public double getPriceCostUYU() {
        return priceCostUYU;
    }

    public void setPriceCostUYU(double priceCostUYU) {
        this.priceCostUYU = priceCostUYU;
    }

    public double getPriceCostUSD() {
        return priceCostUSD;
    }

    public void setPriceCostUSD(double priceCostUSD) {
        this.priceCostUSD = priceCostUSD;
    }

    public double getPriceEditProduct() {
        return priceEditProduct;
    }

    public void setPriceEditProduct(double priceEditProduct) {
        this.priceEditProduct = priceEditProduct;
    }
}
