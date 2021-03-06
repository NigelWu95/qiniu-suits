package com.qiniu.process.qiniu;

import com.qiniu.config.PropertiesFile;
import com.qiniu.storage.Configuration;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeMimeTest {

    @Test
    public void testSingle() throws IOException {
        PropertiesFile propertiesFile = new PropertiesFile("resources/.application.properties");
        String accessKey = propertiesFile.getValue("ak");
        String secretKey = propertiesFile.getValue("sk");
        String bucket = propertiesFile.getValue("bucket");
        ChangeMime changeMime = new ChangeMime(accessKey, secretKey, new Configuration(), bucket, "text/test", null,
                "mime=text/plain&a=b"
//                null
        );
        Map<String, String> map = new HashMap<String, String>(){{
            put("key", "qiniu_success_1.txt");
        }};
        try {
            String result = changeMime.processLine(map);
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBatch() throws IOException {
        PropertiesFile propertiesFile = new PropertiesFile("resources/.application.properties");
        String accessKey = propertiesFile.getValue("ak");
        String secretKey = propertiesFile.getValue("sk");
        String bucket = propertiesFile.getValue("bucket");
        ChangeMime changeMime = new ChangeMime(accessKey, secretKey, new Configuration(), bucket, "text/test", null,
                "mime=text/plain"
                , "../temp"
        );
        List<Map<String, String>> list = new ArrayList<>();
        list.add(new HashMap<String, String>(){{ put("key", "qiniu_success_1.txt"); }});
        list.add(new HashMap<String, String>(){{ put("key", "qiniu_success_2.txt"); }});
        list.add(new HashMap<String, String>(){{ put("key", "qiniu_success_3.txt"); }});
        list.add(new HashMap<String, String>(){{ put("key", "qiniu_success_9.txt"); }});
        try {
            changeMime.processLine(list);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            changeMime.closeResource();
        }
    }
}