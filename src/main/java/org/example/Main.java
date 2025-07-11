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
                    log();
                    break;
                case "commit":
                    System.out.println("чтобы сохранить изменения, используйте \"bit commit -m '<текст сообщения>'\"");
                    break;
                case "checkout":
                    System.out.println("чтобы переключиться на другой коммит, используйте \"bit checkout -h <ваш коммит>\"");
                    break;
                case "test":

                    break;
            }
        } else if (args.length == 3) {
            switch (args[0]) {
                case "commit":
                    if (args[1].equals("-m")) commit(args[2]);
                    break;
                case "checkout":
                    if (args[1].equals("-h")) checkout(args[2]);

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

                    String hash = Chunk.makeHash(formattedDate.getBytes());

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
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            StringBuilder res = new StringBuilder();
            for (String param : params) {
                if (param == null) break;
                else res.append(param).append('\t');
            }
            res.append('\n');

            fw.append(res.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            String pathForCopy = COMMITS_DIR +
                    '/' + commitHash +
                    '/' + file.getAbsolutePath().replace(CURRENT_DIR, "");

            if (file.isDirectory()) {
                File copyDir = new File(pathForCopy);
                copyDir.mkdir();
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {

                    File copyFile = new File(pathForCopy);
                    copyFile.createNewFile();

                    byte[] buffer = new byte[64];

                    try (FileWriter fw = new FileWriter(copyFile)) {

                        while (fis.read(buffer) != -1) {

                            Chunk chunk = new Chunk(buffer);
                            File chunkFile = new File(OBJECTS_DIR + '/' + chunk.getHash());

//                            if (!chunkFile.createNewFile()) System.out.println("такой чанк уже есть");
                            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                                fos.write(chunk.getBytes());
                            }

                            fw.append(chunk.getHash());
                            Arrays.fill(buffer, (byte) 0);
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

    public static void log() {
        File logFile = new File(LOG_FILE);
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            System.err.println("ошибка чтения log-файла");
        }
    }

    public static void checkout(String commitHash) {
        File commitDir = new File(COMMITS_DIR + '/' + commitHash);
        if (commitDir.exists()) {
            clearDir();

            Stack<File> stack = new Stack<>();
            try {
                stack.addAll(List.of(commitDir.listFiles()));
            } catch (Exception e) {
                return;
            }

            while (!stack.isEmpty()) {
                File file = stack.pop();
                if (file.isDirectory()) {
                    File newDir = new File(file.getAbsolutePath().replace(COMMITS_DIR + '/' + commitHash, CURRENT_DIR));
                    newDir.mkdir();
                } else {
                    File newfile = new File(file.getAbsolutePath().replace(COMMITS_DIR + '/' + commitHash, CURRENT_DIR));
                    try (FileOutputStream fos = new FileOutputStream(newfile)) {
//                        TODO
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    stack.addAll(List.of(file.listFiles()));
                } catch (Exception ignored) {
                }
            }

        } else System.out.println("такого коммита не существует");

    }

    public static void clearDir() {
        File root = new File(CURRENT_DIR);

        Stack<File> stack = new Stack<>();
        try {
            for (File child : root.listFiles()) {
                if (!child.getName().equals(".bit")) {
                    stack.push(child);
                }
            }
        } catch (NullPointerException e) {
            return;
        }

        while (!stack.isEmpty()) {
            File file = stack.peek();
            if (!file.delete()) {
                try {
                    for (File child : file.listFiles()) {
                        if (!child.delete() && child.isDirectory()) {
                            stack.push(child);
                        }
                    }
                } catch (NullPointerException ignored) {
                }
            } else stack.pop();
        }
    }

}
