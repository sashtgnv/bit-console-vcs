package org.example;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

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
        if (args.length == 1) {
            switch (args[0]) {
                case "init":
                    init();
                    break;
                case "log":
                    //TODO
                    break;
                case "test":
                    System.out.println(getLastCommitDateTime());
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0]) {
                case "commit":
                    if (args[1].equals("-m")) commit(args[2]);
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
            System.err.println("репозиторий уже был создан ранее");
        }
    }

    public static void commit(String message) {
        File bitDir = new File(BIT_DIR);
        boolean isInit = bitDir.exists();
        if (isInit) {
            if (canMakeCommit()) {
                try {
                    LocalDateTime date = LocalDateTime.now();
                    String formattedDate = date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));

                    String hash = Integer.toHexString(date.hashCode());

                    File commit = new File(BIT_DIR + '/' + hash);
                    commit.mkdir();

                    String[] params = {formattedDate, hash, message};
                    changeLogFile(params);

                    System.out.println("изменения сохранены " + formattedDate);
                } catch (IOException e) {
                    System.err.println("ошибка создания коммита");
                }
            } else {
                System.err.println("сохранять нечего");
            }
        } else {
            System.err.println("сначала создайте репозиторий коммандой \"git init\" ");
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
        try (FileWriter fos = new FileWriter(LOG_FILE, true)) {
            String res = "";
            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) break;
                else res += params[i] + '\t';
            }
            res += '\n';

            fos.write(res);
        }
    }

    public static boolean canMakeCommit() {

        File log = new File(LOG_FILE);
        if (log.exists() && log.length()==0) {
            return true;
        }

        File root = new File(CURRENT_DIR);
        LocalDateTime date = getLastCommitDateTime();
        boolean flag = false;

        assert date != null;
        long commitMillis = date.atZone(ZoneOffset.systemDefault()).toInstant().toEpochMilli();

        Queue<File> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            File file = queue.poll();
            long lastMod = file.lastModified();
            if (lastMod > commitMillis) {
                flag = true;
                break;
            } else {
                File[] children;
                try {
                    children = file.listFiles();
                    for (File child : children) {
                        if (child.getName().equals(".bit")) continue;
                        queue.add(child);
                    }
                } catch (Exception e) {}

            }
        }

        return flag;
    }

    public static LocalDateTime getLastCommitDateTime() {

        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            LocalDateTime date = null;
            while ((line = reader.readLine()) != null) {
                date = LocalDateTime.parse(
                        line.split("\t")[0],
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            }
            return date;

        } catch (IOException e) {
            System.err.println("ошибка во время парсинга log-файла");
//            e.printStackTrace();
            return null;
        }
    }
}
