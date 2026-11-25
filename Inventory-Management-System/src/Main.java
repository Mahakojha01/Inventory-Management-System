import com.sun.net.httpserver.HttpServer;
import server.InventoryController;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new InventoryController());
        server.setExecutor(null);
        server.start();
        System.out.println("============================================================");
        System.out.println(" Inventory Management System (Java + Advanced DSA) Started!");
        System.out.println(" Open in Browser: http://localhost:" + port);
        System.out.println("============================================================");
    }
}
