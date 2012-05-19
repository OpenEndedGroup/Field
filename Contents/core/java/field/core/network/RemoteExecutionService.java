package field.core.network;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RemoteExecutionService extends Remote {

	public void execute(String fragment, RemoteExecutionServiceHost host) throws RemoteException;
	
	public void addActive() throws RemoteException;
	public void removeActive()throws RemoteException;
	
	public Object getData(String name) throws RemoteException;
	public void setData(String name, Object value) throws RemoteException;
	
	public void dieNow() throws RemoteException;
	
	public boolean check() throws RemoteException;
}
