package org.example.coffee.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StringUtils {
    public static String getStringFromList(List<String> imageUrls) {
        if(Objects.isNull(imageUrls) || imageUrls.isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        Integer index = 0;
        for(String imageUrl : imageUrls) {
            result.append(imageUrl);
            if(index != imageUrls.size() - 1) {
                result.append(";");
            }
            index++;
        }
        return result.toString();
    }

    public static List<String> getListFromString(String imageUrls) {
        if(Objects.isNull(imageUrls)) {
            return new ArrayList<>();
        }
        return Arrays.asList(imageUrls.split(";"));
    }
}
