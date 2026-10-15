package client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import network.ClientCallback;
import network.GameView;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {

    private final View view;

    public ClientCallbackImpl(View view) throws RemoteException {
        super();
        this.view = view;
    }

    @Override
    public void onGameUpdate(GameView gameView) throws RemoteException { //metodo chiamato dal server
        view.showGame(gameView);
    }

    @Override
    public void onMessage(String text) throws RemoteException { //metodo chiamato dal server
        view.showMessage(text);
    }
    
}
