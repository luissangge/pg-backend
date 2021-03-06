package uy.com.pepeganga.business.common.utils.methods;

import uy.com.pepeganga.business.common.entities.Image;
import uy.com.pepeganga.business.common.entities.ImagePublicationMeli;

import java.util.List;

public class BurbbleSort {

    public static List<Image> burbbleLowerToHigherByImages(List<Image> A) {
        int i, j;
        Image ima;
        for (i = 0; i < A.size() - 1; i++) {
            for (j = 0; j < A.size() - i - 1; j++) {
                if (A.get(j + 1).getOrder() < A.get(j).getOrder()) {
                    ima = A.get(j + 1);
                    A.set(j + 1, A.get(j));
                    A.set(j, ima);
                }
            }
        }
        return A;
    }

    public static List<ImagePublicationMeli> burbbleLowerToHigherByImagesDetails(List<ImagePublicationMeli> A) {
        int i, j;
        ImagePublicationMeli ima;
        for (i = 0; i < A.size() - 1; i++) {
            for (j = 0; j < A.size() - i - 1; j++) {
                if (A.get(j + 1).getOrder() < A.get(j).getOrder()) {
                    ima = A.get(j + 1);
                    A.set(j + 1, A.get(j));
                    A.set(j, ima);
                }
            }
        }
        return A;
    }
}
