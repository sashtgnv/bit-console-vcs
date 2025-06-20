package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Main {

    public static String CURRENT_DIR;
    public static String BIT_DIR;
    public static String LOG_FILE;

    static {
        CURRENT_DIR = System.getProperty("user.dir");
        BIT_DIR = CURRENT_DIR + "/.bit";
        LOG_FILE = BIT_DIR + "/log";
    }

    public static void main(String args[]) {
        if (args.length == 1 ) {
            switch (args[0]) {
                case "init":
                    init();
                    break;
                case "log":
                    //TODO
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0]){
                case "commit":
                    if (args[1].equals( "-m")) commit(args[2]);
                    break;
                case "checkout":
                    //TODO
                    break;
            }
        }
    }

    public static void init() {
        File bitDir = new File(BIT_DIR);
        boolean created = bitDir.mkdir();
        if (created) {
            System.out.println("создан новый репозиторий");

            createLogFile();
        } else {
            System.out.println("репрзиторий уже был создан ранее");
        }
    }

    public static void commit(String message) {
        File bitDir = new File(CURRENT_DIR + "/.bit");
        boolean isInit = bitDir.exists();
        if (isInit) {
            try {
                LocalDateTime date = LocalDateTime.now();
                String formattedDate = date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));

                String hash = Integer.toHexString(formattedDate.hashCode());

                File commit = new File(BIT_DIR + '/' + hash);
                commit.mkdir();

                String[] params = {formattedDate, hash, message};
                changeLogFile(params);

                System.out.println("изменения сохранены " + formattedDate);
            } catch (IOException e) {
                System.err.println("ошибка создания коммита");
            }
        } else {
            System.out.println("сначала создайте репозиторий коммандой \"git init\" ");
        }
    }

    public static void createLogFile() {
        File logFile = new File(LOG_FILE);
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            System.err.println("ошибка создания log-файла");
        }
    }

    public static void changeLogFile(String[] params) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(LOG_FILE, true)) {
            String res = "";
            for (int i = 0; i < params.length; i++) {
                if (params[i]==null) break;
                else res+=params[i]+'\t';
            }
            res+='\n';

            byte[] bytes = res.getBytes();
            fos.write(bytes);
        }
    }
}
