package org.wildtype;

public class App {
    public static void main(String[] args) throws Exception {
        try (var pipeline = new Pipeline()) {
            pipeline.run();
        }
    }
}
