package field.core.network;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteExecutionServiceHost extends Remote{

	public void print(String id, String text) throws RemoteException;
	public void printError(String id, String text) throws RemoteException;
	
	public Object getData(String id, String name) throws RemoteException;
	public void setData(String id, String name, Object value) throws RemoteException;
	
	
}
