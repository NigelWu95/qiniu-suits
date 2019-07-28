package com.qiniu.util;

import com.aliyun.oss.model.OSSObjectSummary;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qcloud.cos.model.COSObjectSummary;
import com.qiniu.interfaces.KeyValuePair;
import com.qiniu.sdk.FileItem;
import com.qiniu.storage.model.FileInfo;

import java.io.IOException;
import java.util.*;

public final class LineUtils {

    final static public Set<String> etagFields = new HashSet<String>(){{
        add("hash");
        add("etag");
    }};

    final static public Set<String> sizeFields = new HashSet<String>(){{
        add("size");
        add("fsize");
    }};

    final static public Set<String> datetimeFields = new HashSet<String>(){{
        add("datetime");
        add("timestamp");
        add("putTime");
    }};

    final static public Set<String> timestampFields = new HashSet<String>(){{
        add("datetime");
        add("timestamp");
        add("putTime");
    }};

    final static public Set<String> mimeFields = new HashSet<String>(){{
        add("mime");
        add("mimeType");
    }};

    final static public Set<String> typeFields = new HashSet<String>(){{
        add("type");
    }};

    final static public Set<String> statusFields = new HashSet<String>(){{
        add("status");
    }};

    final static public Set<String> md5Fields = new HashSet<String>(){{
        add("md5");
    }};

    final static public Set<String> ownerFields = new HashSet<String>(){{
        add("owner");
        add("endUser");
    }};

    final static public Set<String> intFields = new HashSet<String>(){{
        addAll(statusFields);
    }};

    final static public Set<String> longFields = new HashSet<String>(){{
        addAll(sizeFields);
        addAll(timestampFields);
    }};

    // 为了保证字段按照设置的顺序来读取，故使用 ArrayList
    final static public List<String> defaultFileFields = new ArrayList<String>(){{
        add("key");
        add("etag");
        add("size");
        add("datetime");
        add("mime");
        add("type");
        add("status");
        add("md5");
        add("owner");
    }};

    final static public Set<String> statFileFields = new HashSet<String>(){{
        add("key");
        add("hash");
        add("fsize");
        add("putTime");
        add("mimeType");
        add("type");
        add("status");
        add("md5");
        add("endUser");
        add("_id");
    }};

    public static List<String> getFields(List<String> fields, List<String> rmFields) {
        if (rmFields == null) return fields;
        for (String rmField : rmFields) {
            if (LineUtils.etagFields.contains(rmField)) fields.removeAll(LineUtils.etagFields);
            else if (LineUtils.sizeFields.contains(rmField)) fields.removeAll(LineUtils.sizeFields);
            else if (LineUtils.datetimeFields.contains(rmField)) fields.removeAll(LineUtils.datetimeFields);
            else if (LineUtils.timestampFields.contains(rmField)) fields.removeAll(LineUtils.timestampFields);
            else if (LineUtils.mimeFields.contains(rmField)) fields.removeAll(LineUtils.mimeFields);
            else if (LineUtils.statusFields.contains(rmField)) fields.removeAll(LineUtils.statusFields);
            else if (LineUtils.typeFields.contains(rmField)) fields.removeAll(LineUtils.typeFields);
            else if (LineUtils.md5Fields.contains(rmField)) fields.removeAll(LineUtils.md5Fields);
            else if (LineUtils.ownerFields.contains(rmField)) fields.removeAll(LineUtils.ownerFields);
            else fields.remove(rmField);
        }
        return fields;
    }

    public static Map<String, String> getReversedIndexMap(Map<String, String> map, List<String> rmFields) {
        Map<String, String> indexMap = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) indexMap.put(entry.getValue(), entry.getKey());
        if (rmFields == null) return indexMap;
        for (String rmField : rmFields) indexMap.remove(rmField);
        return indexMap;
    }

    public static <T> T getPair(FileInfo fileInfo, Map<String, String> indexMap, KeyValuePair<String, T> pair)
            throws IOException {
        if (fileInfo == null || fileInfo.key == null) throw new IOException("empty fileInfo or key.");
        for (String index : indexMap.keySet()) {
            switch (index) {
                case "key": pair.put(indexMap.get(index), fileInfo.key); break;
                case "hash":
                case "etag": pair.put(indexMap.get(index), fileInfo.hash); break;
                case "size":
                case "fsize": pair.put(indexMap.get(index), String.valueOf(fileInfo.fsize)); break;
                case "datetime": pair.put(indexMap.get(index), DatetimeUtils.stringOf(fileInfo.putTime, 10000000)); break;
                case "timestamp":
                case "putTime": pair.put(indexMap.get(index), String.valueOf(fileInfo.putTime)); break;
                case "mime":
                case "mimeType": pair.put(indexMap.get(index), fileInfo.mimeType); break;
                case "type": pair.put(indexMap.get(index), String.valueOf(fileInfo.type)); break;
                case "status": pair.put(indexMap.get(index), String.valueOf(fileInfo.status)); break;
                case "md5": pair.put(indexMap.get(index), fileInfo.md5); break;
                case "owner":
                case "endUser": if (fileInfo.endUser != null) pair.put(indexMap.get(index), fileInfo.endUser); break;
                default: throw new IOException("Qiniu fileInfo doesn't have field: " + index);
            }
        }
        if (pair.size() == 0) throw new IOException("empty result keyValuePair.");
        return pair.getProtoEntity();
    }

    public static <T> T getPair(COSObjectSummary cosObject, Map<String, String> indexMap, KeyValuePair<String, T> pair)
            throws IOException {
        if (cosObject == null || cosObject.getKey() == null) throw new IOException("empty cosObjectSummary or key.");
        for (String index : indexMap.keySet()) {
            switch (index) {
                case "key": pair.put(indexMap.get(index), cosObject.getKey()); break;
                case "hash":
                case "etag": pair.put(indexMap.get(index), cosObject.getETag()); break;
                case "size":
                case "fsize": pair.put(indexMap.get(index), String.valueOf(cosObject.getSize())); break;
                case "datetime": pair.put(indexMap.get(index), DatetimeUtils.stringOf(cosObject.getLastModified())); break;
                case "timestamp":
                case "putTime": pair.put(indexMap.get(index), String.valueOf(cosObject.getLastModified().getTime())); break;
//                case "mime": case "mimeType": break;
                case "type": pair.put(indexMap.get(index), cosObject.getStorageClass()); break;
//                case "status": case "md5": break;
                case "owner":
                case "endUser": if (cosObject.getOwner() != null)
                    pair.put(indexMap.get(index), cosObject.getOwner().getDisplayName()); break;
                default: throw new IOException("COSObjectSummary doesn't have field: " + index);
            }
        }
        if (pair.size() == 0) throw new IOException("empty result keyValuePair.");
        return pair.getProtoEntity();
    }

    public static <T> T getPair(OSSObjectSummary ossObject, Map<String, String> indexMap, KeyValuePair<String, T> pair)
            throws IOException {
        if (ossObject == null || ossObject.getKey() == null) throw new IOException("empty ossObjectSummary or key.");
        for (String index : indexMap.keySet()) {
            switch (index) {
                case "key": pair.put(indexMap.get(index), ossObject.getKey()); break;
                case "hash":
                case "etag": pair.put(indexMap.get(index), ossObject.getETag()); break;
                case "size":
                case "fsize": pair.put(indexMap.get(index), String.valueOf(ossObject.getSize())); break;
                case "datetime": pair.put(indexMap.get(index), DatetimeUtils.stringOf(ossObject.getLastModified())); break;
                case "timestamp":
                case "putTime": pair.put(indexMap.get(index), String.valueOf(ossObject.getLastModified().getTime())); break;
//                case "mime": case "mimeType": break;
                case "type": pair.put(indexMap.get(index), ossObject.getStorageClass()); break;
//                case "status": case "md5": break;
                case "owner":
                case "endUser": if (ossObject.getOwner() != null)
                    pair.put(indexMap.get(index), ossObject.getOwner().getDisplayName()); break;
                default: throw new IOException("OSSObjectSummary doesn't have field: " + index);
            }
        }
        if (pair.size() == 0) throw new IOException("empty result keyValuePair.");
        return pair.getProtoEntity();
    }

    public static <T> T getPair(S3ObjectSummary s3Object, Map<String, String> indexMap, KeyValuePair<String, T> pair)
            throws IOException {
        if (s3Object == null || s3Object.getKey() == null) throw new IOException("empty s3ObjectSummary or key.");
        for (String index : indexMap.keySet()) {
            switch (index) {
                case "key": pair.put(indexMap.get(index), s3Object.getKey()); break;
                case "hash":
                case "etag": pair.put(indexMap.get(index), s3Object.getETag()); break;
                case "size":
                case "fsize": pair.put(indexMap.get(index), String.valueOf(s3Object.getSize())); break;
                case "datetime": pair.put(indexMap.get(index), DatetimeUtils.stringOf(s3Object.getLastModified())); break;
                case "timestamp":
                case "putTime": pair.put(indexMap.get(index), String.valueOf(s3Object.getLastModified().getTime())); break;
//                case "mime": case "mimeType": break;
                case "type": pair.put(indexMap.get(index), s3Object.getStorageClass()); break;
//                case "status": case "md5": break;
                case "owner":
                case "endUser": if (s3Object.getOwner() != null) pair.put(indexMap.get(index),
                        s3Object.getOwner().getDisplayName()); break;
                default: throw new IOException("S3ObjectSummary doesn't have field: " + index);
            }
        }
        if (pair.size() == 0) throw new IOException("empty result keyValuePair.");
        return pair.getProtoEntity();
    }

    public static <T> T getPair(FileItem fileItem, Map<String, String> indexMap, KeyValuePair<String, T> pair)
            throws IOException {
        if (fileItem == null || fileItem.key == null) throw new IOException("empty fileItem or key.");
        for (String index : indexMap.keySet()) {
            switch (index) {
                case "key": pair.put(indexMap.get(index), fileItem.key); break;
//                case "hash": case "etag": break;
                case "size":
                case "fsize": pair.put(indexMap.get(index), String.valueOf(fileItem.size)); break;
                case "datetime": pair.put(indexMap.get(index), DatetimeUtils.stringOf(fileItem.timeSeconds)); break;
                case "timestamp":
                case "putTime": pair.put(indexMap.get(index), String.valueOf(fileItem.timeSeconds)); break;
                case "mime":
                case "mimeType": pair.put(indexMap.get(index), fileItem.attribute); break;
//                case "type": case "status": case "md5": case "owner": case "endUser": break;
                default: throw new IOException("Upyun fileItem doesn't have field: " + index);
            }
        }
        if (pair.size() == 0) throw new IOException("empty result keyValuePair.");
        return pair.getProtoEntity();
    }

    public static <T> T getPair(JsonObject json, Map<String, String> indexMap, KeyValuePair<String, T> pair)
            throws IOException {
        if (json == null) throw new IOException("empty JsonObject.");
        JsonElement jsonElement;
        for (String index : indexMap.keySet()) {
            jsonElement = json.get(index);
            if (jsonElement == null || jsonElement instanceof JsonNull) {
                throw new IOException("the index: " + index + " can't be found in " + json);
            } else {
                pair.put(indexMap.get(index), JsonUtils.toString(jsonElement));
            }
        }
        if (pair.size() == 0) throw new IOException("empty result keyValuePair.");
        return pair.getProtoEntity();
    }

    public static <T> T getPair(String line, Map<String, String> indexMap, KeyValuePair<String, T> pair) throws IOException {
        if (line == null) throw new IOException("empty json line.");
        JsonObject parsed = new JsonParser().parse(line).getAsJsonObject();
        return getPair(parsed, indexMap, pair);
    }

    public static <T> T getPair(String line, String separator, Map<String, String> indexMap, KeyValuePair<String, T> pair)
            throws IOException {
        if (line == null) throw new IOException("empty string line.");
        String[] items = line.split(separator);
        int position;
        for (String index : indexMap.keySet()) {
            position = Integer.valueOf(index);
            if (items.length > position) pair.put(indexMap.get(index), items[position]);
            else throw new IOException("the index: " + index + " can't be found in " + line);
        }
        if (pair.size() == 0) throw new IOException("empty result keyValuePair.");
        return pair.getProtoEntity();
    }

    public static String toFormatString(FileInfo fileInfo, String separator, List<String> fields) throws IOException {
        if (fileInfo == null || fileInfo.key == null) throw new IOException("empty file or key.");
        StringBuilder converted = new StringBuilder();
        for (String field : fields) {
            switch (field) {
                case "key": converted.append(fileInfo.key).append(separator); break;
                case "hash":
                case "etag": converted.append(fileInfo.hash).append(separator); break;
                case "size":
                case "fsize": converted.append(fileInfo.fsize).append(separator); break;
                case "datetime": converted.append(DatetimeUtils.stringOf(fileInfo.putTime, 10000000)).append(separator);
                    break;
                case "timestamp":
                case "putTime": converted.append(fileInfo.putTime).append(separator); break;
                case "mime":
                case "mimeType": converted.append(fileInfo.mimeType).append(separator); break;
                case "type": converted.append(fileInfo.type).append(separator); break;
                case "status": converted.append(fileInfo.status).append(separator); break;
                case "md5": converted.append(fileInfo.md5).append(separator); break;
                case "owner":
                case "endUser": if (fileInfo.endUser != null) converted.append(fileInfo.endUser).append(separator); break;
                default: throw new IOException("Qiniu fileInfo has not field: " + field);
            }
        }
        if (converted.length() == 0) throw new IOException("empty result string.");
        return converted.substring(0, converted.length() - separator.length());
    }

    public static String toFormatString(COSObjectSummary cosObject, String separator, List<String> fields) throws IOException {
        if (cosObject == null || cosObject.getKey() == null) throw new IOException("empty cosObjectSummary or key.");
        StringBuilder converted = new StringBuilder();
        for (String field : fields) {
            switch (field) {
                case "key": converted.append(cosObject.getKey()).append(separator); break;
                case "hash":
                case "etag": converted.append(cosObject.getETag()).append(separator); break;
                case "size":
                case "fsize": converted.append(cosObject.getSize()).append(separator); break;
                case "datetime": converted.append(DatetimeUtils.stringOf(cosObject.getLastModified())).append(separator);
                    break;
                case "timestamp":
                case "putTime": converted.append(cosObject.getLastModified().getTime()).append(separator); break;
                case "type": converted.append(cosObject.getStorageClass()).append(separator); break;
                case "owner":
                case "endUser": if (cosObject.getOwner() != null)
                    converted.append(cosObject.getOwner().getDisplayName()).append(separator); break;
                default: throw new IOException("COSObjectSummary has not field: " + field);
            }
        }
        if (converted.length() == 0) throw new IOException("empty result string.");
        return converted.substring(0, converted.length() - separator.length());
    }

    public static String toFormatString(OSSObjectSummary ossObject, String separator, List<String> fields) throws IOException {
        if (ossObject == null || ossObject.getKey() == null) throw new IOException("empty ossObjectSummary or key.");
        StringBuilder converted = new StringBuilder();
        for (String field : fields) {
            switch (field) {
                case "key": converted.append(ossObject.getKey()).append(separator); break;
                case "hash":
                case "etag": converted.append(ossObject.getETag()).append(separator); break;
                case "size":
                case "fsize": converted.append(ossObject.getSize()).append(separator); break;
                case "datetime": converted.append(DatetimeUtils.stringOf(ossObject.getLastModified())).append(separator);
                    break;
                case "timestamp":
                case "putTime": converted.append(ossObject.getLastModified().getTime()).append(separator); break;
                case "type": converted.append(ossObject.getStorageClass()).append(separator); break;
                case "owner":
                case "endUser": if (ossObject.getOwner() != null)
                    converted.append(ossObject.getOwner().getDisplayName()).append(separator); break;
                default: throw new IOException("OSSObjectSummary has not field: " + field);
            }
        }
        if (converted.length() == 0) throw new IOException("empty result string.");
        return converted.substring(0, converted.length() - separator.length());
    }

    public static String toFormatString(S3ObjectSummary s3Object, String separator, List<String> fields)
            throws IOException {
        if (s3Object == null || s3Object.getKey() == null) throw new IOException("empty S3ObjectSummary or key.");
        StringBuilder converted = new StringBuilder();
        for (String field : fields) {
            switch (field) {
                case "key": converted.append(s3Object.getKey()).append(separator); break;
                case "hash":
                case "etag": converted.append(s3Object.getETag()).append(separator); break;
                case "size":
                case "fsize": converted.append(s3Object.getSize()).append(separator); break;
                case "datetime": converted.append(DatetimeUtils.stringOf(s3Object.getLastModified())).append(separator);
                    break;
                case "timestamp":
                case "putTime": converted.append(s3Object.getLastModified().getTime()).append(separator); break;
                case "type": converted.append(s3Object.getStorageClass()).append(separator); break;
                case "owner":
                case "endUser": if (s3Object.getOwner() != null)
                    converted.append(s3Object.getOwner().getDisplayName()).append(separator); break;
                default: throw new IOException("S3ObjectSummary has not field: " + field);
            }
        }
        if (converted.length() == 0) throw new IOException("empty result string.");
        return converted.substring(0, converted.length() - separator.length());
    }

    public static String toFormatString(FileItem fileItem, String separator, List<String> fields) throws IOException {
        if (fileItem == null || fileItem.key == null) throw new IOException("empty fileItem or key.");
        StringBuilder converted = new StringBuilder();
        for (String field : fields) {
            switch (field) {
                case "key": converted.append(fileItem.key).append(separator); break;
                case "size":
                case "fsize": converted.append(fileItem.size).append(separator); break;
                case "datetime": converted.append(DatetimeUtils.stringOf(fileItem.timeSeconds)).append(separator); break;
                case "timestamp":
                case "putTime": converted.append(fileItem.timeSeconds).append(separator); break;
                case "mime":
                case "mimeType": converted.append(fileItem.attribute).append(separator); break;
                default: throw new IOException("Upyun fileItem has not field: " + field);
            }
        }
        if (converted.length() == 0) throw new IOException("empty result string.");
        return converted.substring(0, converted.length() - separator.length());
    }

    public static String toFormatString(Map<String, String> line, List<String> fields) throws IOException {
        if (line == null) throw new IOException("empty string map.");
        JsonObject converted = new JsonObject();
        String value;
        for (String field : fields) {
            value = line.get(field);
            if (value != null) {
                if (longFields.contains(field)) converted.addProperty(field, Long.valueOf(value));
                else if (intFields.contains(field)) converted.addProperty(field, Integer.valueOf(value));
                else converted.addProperty(field, value);
            } else {
                throw new IOException("the field: " + field + " can't be found in " + line);
            }
        }
        if (converted.size() == 0) throw new IOException("empty result string.");
        return converted.toString();
    }

    public static String toFormatString(Map<String, String> line, String separator, List<String> fields) throws IOException {
        if (line == null) throw new IOException("empty string map.");
        StringBuilder converted = new StringBuilder();
        String value;
        for (String field : fields) {
            value = line.get(field);
            if (value != null) {
                if (longFields.contains(field)) converted.append(Long.valueOf(value)).append(separator);
                else if (intFields.contains(field)) converted.append(Integer.valueOf(value)).append(separator);
                else converted.append(value).append(separator);
            } else {
                throw new IOException("the field: " + field + " can't be found in " + line);
            }
        }
        if (converted.length() == 0) throw new IOException("empty result string.");
        return converted.substring(0, converted.length() - separator.length());
    }

    public static String toFormatString(JsonObject json, String separator, List<String> fields) throws IOException {
        if (json == null) throw new IOException("empty JsonObject.");
        StringBuilder converted = new StringBuilder();
        JsonElement value;
        for (String field : fields) {
            value = json.get(field);
            if (value == null || value instanceof JsonNull) {
                if (!statFileFields.contains(field))
                    throw new IOException("the field: " + field + " can't be found in " + json);
            } else {
                if (longFields.contains(field)) converted.append(value.getAsLong()).append(separator);
                else if (intFields.contains(field)) converted.append(value.getAsInt()).append(separator);
                else converted.append(value.getAsString()).append(separator);
            }
        }
        if (converted.length() == 0) throw new IOException("empty result string.");
        return converted.substring(0, converted.length() - separator.length());
    }
}
