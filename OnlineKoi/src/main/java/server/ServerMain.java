package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {
    public static void main(String[] args) throws RemoteException{

        LobbyImpl lobby = new LobbyImpl();

        Registry registry = LocateRegistry.createRegistry(1099); //1099 è la porta standard per RMI
        registry.rebind("KoiGame", lobby); //KoiGame è il name che i client devono usare per il lookup

        System.out.println("Server ready");

    }
    
}
