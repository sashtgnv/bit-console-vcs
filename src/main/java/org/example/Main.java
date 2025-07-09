package org.example;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.Stack;

public class Main {

    public static String CURRENT_DIR;
    public static String BIT_DIR;
    public static String OBJECTS_DIR;
    public static String COMMITS_DIR;
    public static String LOG_FILE;

    static {
        CURRENT_DIR = System.getProperty("user.dir");
        BIT_DIR = CURRENT_DIR + "/.bit";
        LOG_FILE = BIT_DIR + "/log";
        OBJECTS_DIR = BIT_DIR + "/objects";
        COMMITS_DIR = BIT_DIR + "/commits";
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            switch (args[0]) {
                case "init":
                    init();
                    break;
                case "log":
                    //TODO
                    break;
                case "test":
                    saveChanges("81c9a88c");
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
            File logFile = new File(LOG_FILE);
            File objDir = new File(OBJECTS_DIR);
            File comDir = new File(COMMITS_DIR);
            try {
                logFile.createNewFile();
                objDir.mkdir();
                comDir.mkdir();
            } catch (IOException e) {
                System.err.println("ошибка инициализации репозитория");
            }
        } else {
            System.err.println("репозиторий уже был создан ранее");
        }
    }

    public static boolean isInit() {
        File bitDir = new File(BIT_DIR);
        File logFile = new File(LOG_FILE);
        File objDir = new File(OBJECTS_DIR);
        File comDir = new File(COMMITS_DIR);
        return bitDir.exists() && bitDir.isHidden() && bitDir.isDirectory() &&
                logFile.exists() && logFile.isFile() &&
                objDir.exists() && objDir.isDirectory() &&
                comDir.exists() && comDir.isDirectory();
    }

    public static void commit(String message) {
        if (isInit()) {
            if (canCommit()) {
                try {
                    LocalDateTime date = LocalDateTime.now();
                    String formattedDate = date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));

                    String hash = Integer.toHexString(date.hashCode());

                    File commit = new File(COMMITS_DIR + '/' + hash);
                    commit.mkdir();
                    saveChanges(hash);

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


    public static void changeLogFile(String[] params) throws IOException {
        try (FileWriter fos = new FileWriter(LOG_FILE, true)) {
            StringBuilder res = new StringBuilder();
            for (String param : params) {
                if (param == null) break;
                else res.append(param).append('\t');
            }
            res.append('\n');

            fos.write(res.toString());
        }
    }

    /*проверяет в ширину дерево файлов, есть ли изменения*/
    public static boolean canCommit() {

        File log = new File(LOG_FILE);
        if (log.exists() && log.length() == 0) {
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
            }

            File[] children;
            try {
                children = file.listFiles();
                for (File child : children) {
                    if (child.getName().equals(".bit")) continue;
                    queue.add(child);
                }
            } catch (Exception ignored) {
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

    public static void saveChanges(String commitHash) {
        File root = new File(CURRENT_DIR);

        Stack<File> stack = new Stack<>();
//        stack.add(root);
        try {
            File[] children = root.listFiles();
            for (File child : children) {
                if (!child.getName().equals(".bit")) stack.push(child);
            }
        } catch (Exception ignored) {
        }

        while (!stack.isEmpty()) {

            File file = stack.pop();

            if (file.isDirectory()) {
                File copyDir = new File(COMMITS_DIR +
                        '/' + commitHash +
                        '/' + file.getAbsolutePath().replace(CURRENT_DIR, ""));
                copyDir.mkdir();
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    System.out.println(file.getName());

                    byte[] buffer = new byte[64];
                    while (fis.read(buffer) != -1) {

                        Chunk chunk = new Chunk(buffer);
                        File chunkFile = new File(OBJECTS_DIR + '/' + chunk.getHash());

//                        if (!chunkFile.createNewFile()) System.out.println("такой чанк уже есть");

                        try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                            fos.write(chunk.getBytes());
                        }


                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                File[] children = file.listFiles();
                for (File child : children) {
                    if (!child.getName().equals(".bit")) stack.push(child);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
