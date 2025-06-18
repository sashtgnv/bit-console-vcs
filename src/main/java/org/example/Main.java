package org.example;

import java.io.File;
import java.io.IOException;
import java.sql.SQLOutput;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Main {

    public static String CURRENT_DIR;
    public static String BIT_DIR;

    static {
        CURRENT_DIR = System.getProperty("user.dir");
        BIT_DIR = CURRENT_DIR + "/.bit";
    }

    public static void main(String args[]) {
        if (args.length > 0) {
            switch (args[0]) {
                case "init":
                    init();
                    break;
                case "commit":
                    commit();
                    break;
            }
        }
    }

    public static void init() {
        File bitDir = new File(BIT_DIR);
        boolean created = bitDir.mkdir();
        if (created) {
            System.out.println("создан новый репозиторий");
        } else {
            System.out.println("репрзиторий уже был создан ранее");
        }
    }

    public static void commit(){
        File bitDir = new File(CURRENT_DIR + "/.bit");
        boolean isInit = bitDir.exists();
        if (isInit) {
            try {
                LocalDateTime date = LocalDateTime.now();
                String formattedDate = date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));

                int hash = Math.abs(formattedDate.hashCode());

                File commit = new File(BIT_DIR + '/'+  hash);
                commit.createNewFile();

                System.out.println("изменения сохранены " + formattedDate);
            } catch (IOException e) {
                System.out.println("ошибка создания коммита");
            }
        } else {
            System.out.println("сначала создайте репозиторий коммандой \"git init\" ");
        }
    }
}
