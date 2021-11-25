package fr.eiffelcorp.ifshare.rmi.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import fr.eiffelcorp.ifshare.rmi.common.IObservator;
import fr.eiffelcorp.ifshare.rmi.common.IProduct;
import fr.eiffelcorp.ifshare.rmi.common.IShop;

@SuppressWarnings("serial")
public class Shop extends UnicastRemoteObject implements IShop {
	private Map<Integer, List<IProduct>> products = new HashMap<>();
	private Map<Integer, IObservator> observators = new HashMap<>();
	private final Object lock = new Object();

	protected Shop() throws RemoteException {
		super();
	}
	
	@Override
	public int registerClient(String name, IObservator observator) throws RemoteException {
		int token = name.hashCode(); 
		int count = 0;
		while (products.containsKey(token)) {
			token = (token / 2) + count;
			count += 1;
		}
		synchronized (lock) {
			products.put(token, new ArrayList<IProduct>());
		}
		addObservator(token, observator);
		return token;
	}
	
	private IProduct searchForProduct(Integer token, String product_name) throws RemoteException {
		for (var product : products.get(token)) {
			if (product.getName().equals(product_name)) {
				return product;
			}
		}
		return null;
	}

	@Override
	public int sellToClient(Integer token, String product_name) throws RemoteException {
		IProduct product = searchForProduct(token, product_name);
		if (product != null) {
			synchronized (lock) {
				observators.get(token).setProduct("");
				products.get(token).remove(product);
			}
			return 0;
		}
		synchronized (lock) {
			observators.get(token).setProduct(product_name);
		}
		return 1;
	}

	@Override
	public int buyFromClient(Integer token, IProduct product) throws RemoteException {
		synchronized (lock) {
			products.get(token).add(product);
		}
		notifyAllObservators(product);
		return 0;
	}
	
	@Override
	public String storeProducts() throws RemoteException {
		var sj = new StringJoiner(", ");
		for (var productsList : products.values()) {
			for (var prod : productsList) {
				sj.add(prod.getInfo());
			}
		}
		return sj.toString();
	}
	
	@Override
	public String clientProducts(Integer token) throws RemoteException {
		var sj = new StringJoiner(", ");
		for (var prod : products.get(token)) {
			sj.add(prod.getInfo());
		}
		return sj.toString();
	}
	
	public void addObservator(Integer token, IObservator observator) throws RemoteException {
		synchronized (lock) {
			this.observators.put(token, observator);
		}
	}
	

    @Override
    public void notifyAllObservators(IProduct product) throws RemoteException {
        for (var observator : observators.values()) {
        	synchronized (lock) {
        		 observator.update(product);
        	}
        }
    }

    @Override
    public void removeAllObservators() throws RemoteException {
    	synchronized (lock) {
    		observators.clear();
    	}
    }

}
