package com.qiniu.convert;

import com.qiniu.interfaces.ILineParser;
import com.qiniu.interfaces.ITypeConvert;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectToMap<E> implements ITypeConvert<E, Map<String, String>> {

    protected ILineParser<E> lineParser;
    private List<String> errorList = new ArrayList<>();

    public Map<String, String> convertToV(E line) throws IOException {
        return lineParser.getItemMap(line);
    }

    public List<Map<String, String>> convertToVList(List<E> lineList) {
        if (lineList == null || lineList.size() == 0) return new ArrayList<>();
        return lineList.stream()
                .map(line -> {
                    try {
                        return lineParser.getItemMap(line);
                    } catch (Exception e) {
                        errorList.add(String.valueOf(line) + "\t" + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Map<String, String>> toVList(List<E> lineList) {
        List<Map<String, String>> mapList = new ArrayList<>();
        if (lineList != null && lineList.size() > 0) {
            for (E line : lineList) {
                try {
                    mapList.add(lineParser.getItemMap(line));
                } catch (Exception e) {
                    errorList.add(String.valueOf(line) + "\t" + e.getMessage());
                }
            }
        }
        return mapList;
    }

    public int errorSize() {
        return errorList.size();
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public List<String> consumeErrorList() {
        List<String> errors = new ArrayList<>(errorList);
//        Collections.addAll(errors, new String[errorList.size()]);
//        Collections.copy(errors, errorList);
        errorList.clear();
        return errors;
    }
}
