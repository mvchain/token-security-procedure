package com.mvc.security.procedure.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class TextSearchFile {
    static int countFiles = 0;
    static int countFolders = 0;

    public static File[] searchFile(File folder, final String keyWord) {

        File[] subFolders = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile()) {
                    countFiles++;
                } else {
                    countFolders++;
                }
                if (pathname.isDirectory()
                        || (pathname.isFile() && pathname.getName().toLowerCase().contains(keyWord.toLowerCase()))) {
                    return true;
                }
                return false;
            }
        });

        List<File> result = new ArrayList<File>();
        for (int i = 0; i < subFolders.length; i++) {
            if (subFolders[i].isFile()) {
                result.add(subFolders[i]);
            } else {
                File[] foldResult = searchFile(subFolders[i], keyWord);
                for (int j = 0; j < foldResult.length; j++) {
                    result.add(foldResult[j]);
                }
            }
        }

        File[] files = new File[result.size()];
        result.toArray(files);
        return files;
    }
}