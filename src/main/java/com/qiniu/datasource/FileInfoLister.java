package com.qiniu.datasource;

import com.qiniu.interfaces.ILocalFileLister;
import com.qiniu.model.local.FileInfo;
import com.qiniu.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class FileInfoLister implements ILocalFileLister<FileInfo, File> {

    private String name;
    private int limit;
    private String endPrefix;
    private List<FileInfo> fileInfoList;
    private List<File> directories;
    private Iterator<FileInfo> iterator;
    private List<FileInfo> currents;
    private FileInfo last;
    private String truncated;
    private long count;

    private void checkFileInfoList(String start) throws IOException {
        if ((start == null || "".equals(start)) && (endPrefix == null || "".equals(endPrefix))) {
            fileInfoList.sort(Comparator.comparing(fileInfo -> fileInfo.filepath));
        } else if (start == null || "".equals(start)) {
            fileInfoList = fileInfoList.stream()
                    .filter(fileInfo -> fileInfo.filepath.compareTo(endPrefix) <= 0)
                    .sorted().collect(Collectors.toList());
        } else if (endPrefix == null || "".equals(endPrefix)) {
            fileInfoList = fileInfoList.stream()
                    .filter(fileInfo -> fileInfo.filepath.compareTo(start) > 0)
                    .sorted().collect(Collectors.toList());
        } else if (start.compareTo(endPrefix) >= 0) {
            throw new IOException("start filename can not be larger than end filename prefix.");
        } else {
            fileInfoList = fileInfoList.stream()
                    .filter(fileInfo -> fileInfo.filepath.compareTo(start) > 0 && fileInfo.filepath.compareTo(endPrefix) <= 0)
                    .sorted().collect(Collectors.toList());
        }
    }

    public FileInfoLister(File file, boolean checkText, String transferPath, int leftTrimSize, String start,
                          String endPrefix, int limit) throws IOException {
        if (file == null) throw new IOException("input file is null.");
        this.name = file.getPath();
        File[] fs = file.listFiles();
        if (fs == null) throw new IOException("input file is not valid directory: " + file.getPath());
        fileInfoList = new ArrayList<>();
        directories = new ArrayList<>();
        for(File f : fs) {
            if (f.isHidden()) continue;
            if (f.isDirectory()) {
                directories.add(f);
            } else {
                if (checkText) {
                    String type = FileUtils.contentType(f);
                    if (type.startsWith("text") || type.equals("application/octet-stream")) {
                        fileInfoList.add(new FileInfo(f, transferPath, leftTrimSize));
                    }
                } else {
                    fileInfoList.add(new FileInfo(f, transferPath, leftTrimSize));
                }
            }
        }
        this.limit = limit;
        this.endPrefix = endPrefix;
        checkFileInfoList(start);
        currents = new ArrayList<>();
        iterator = fileInfoList.iterator();
        if (iterator.hasNext()) {
            last = iterator.next();
            iterator.remove();
            currents.add(last);
        }
        count = fileInfoList.size();
        file = null;
    }

    public FileInfoLister(String name, List<FileInfo> fileInfoList, String start, String endPrefix, int limit) throws IOException {
        this.name = name;
        if (fileInfoList == null) throw new IOException("init fileInfoList can not be null.");
        this.fileInfoList = fileInfoList;
        this.limit = limit;
        this.endPrefix = endPrefix;
        checkFileInfoList(start);
        currents = new ArrayList<>();
        iterator = fileInfoList.iterator();
        if (iterator.hasNext()) {
            last = iterator.next();
            iterator.remove();
            currents.add(last);
        }
        count = fileInfoList.size();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setEndPrefix(String endPrefix) {
        if (endPrefix != null && !"".equals(endPrefix)) {
            fileInfoList = fileInfoList.stream()
                    .filter(fileInfo -> fileInfo.filepath.compareTo(endPrefix) > 0)
                    .sorted().collect(Collectors.toList());
            iterator = fileInfoList.iterator();
            count = fileInfoList.size();
        }
    }

    @Override
    public String getEndPrefix() {
        return endPrefix;
    }

    @Override
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void listForward() {
        if (last == null) {
            iterator = null;
            currents.clear();
            fileInfoList.clear();
        } else if (count <= limit) {
            last = null;
            iterator = null;
            currents = fileInfoList;
        } else {
            currents.clear();
            while (iterator.hasNext()) {
                if (currents.size() >= limit) break;
                currents.add(iterator.next());
                iterator.remove();
            }
            last = null;
        }
    }

    @Override
    public boolean hasNext() {
        return iterator != null && iterator.hasNext();
    }

    @Override
    public List<FileInfo> currents() {
        return currents;
    }

    @Override
    public String currentEndFilepath() {
        if (truncated != null) return truncated;
        return last.filepath;
    }

    @Override
    public List<File> getDirectories() {
        return directories;
    }

    @Override
    public List<FileInfo> getRemainedFiles() {
        if (iterator == null) return null;
        else return fileInfoList;
    }

    @Override
    public String truncate() {
        truncated = last.filepath;
        last = null;
        return truncated;
    }

    @Override
    public long count() {
        return count;
    }

    @Override
    public void close() {
        iterator = null;
        if (currents.size() > 0) {
            last = currents.get(currents.size() - 1);
            currents.clear();
        }
//        fileInfoList = null;
        currents = null;
    }
}