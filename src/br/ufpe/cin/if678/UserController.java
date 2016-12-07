package br.ufpe.cin.if678;

import static br.ufpe.cin.if678.communication.UserAction.REQUEST_USER_LIST;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import br.ufpe.cin.if678.business.Group;
import br.ufpe.cin.if678.communication.Listener;
import br.ufpe.cin.if678.communication.Reader;
import br.ufpe.cin.if678.communication.ServerAction;
import br.ufpe.cin.if678.communication.Writer;
import br.ufpe.cin.if678.gui.DisplayMessage;
import br.ufpe.cin.if678.threads.ReconnectionThread;
import br.ufpe.cin.if678.util.Pair;
import br.ufpe.cin.if678.util.Tuple;

/**
 * Controla as threads de leitura e escrita do socket de conexão com o servidor
 * 
 * @author Ramon
 */
public class UserController {

	public static final int MAIN_PORT = 6666;

	// Como estamos usando uma classe Singleton, precisamos da variável para
	// salvar a instância
	private static UserController INSTANCE;

	/**
	 * Retorna a instância inicianda da classe
	 * 
	 * @return instância da classe
	 */
	public static UserController getInstance() {
		// Caso seja o primeiro uso, é necessário iniciar a instância
		if (INSTANCE == null) {
			INSTANCE = new UserController();
		}

		return INSTANCE;
	}

	private String serverIP;

	private int userID;
	private InetSocketAddress userAddress;

	private HashMap<Integer, Pair<String, InetSocketAddress>> IDToNameAddress;
	private HashMap<String, Integer> nameToID;
	private HashMap<InetSocketAddress, Integer> addressToID;

	private HashMap<String, List<DisplayMessage>> groupMessages;

	private Listener listener;

	// Threads de leitura e escrita
	private Pair<Reader, Thread> readerPair;
	private Pair<Writer, Thread> writerPair;

	private HashMap<String, Group> groups;

	private UserController() {
		this.IDToNameAddress = new HashMap<Integer, Pair<String, InetSocketAddress>>();
		this.nameToID = new HashMap<String, Integer>();
		this.addressToID = new HashMap<InetSocketAddress, Integer>();

		this.groupMessages = new HashMap<String, List<DisplayMessage>>();

		this.listener = new Listener(this);

		this.groups = new HashMap<String, Group>();
	}

	public void initialize(String serverIP) throws UnknownHostException, IOException {
		this.serverIP = serverIP;

		// Cria o socket no endereço do servidor
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(serverIP, MAIN_PORT), 100);

		userAddress = (InetSocketAddress) socket.getLocalSocketAddress();

		// Inicia os gerenciadores de leitura e escrita
		Reader reader = new Reader(socket);
		Writer writer = new Writer(socket);

		// Inicia as instâncias das threads de leitura e escrita
		Thread readerThread = new Thread(reader);
		Thread writerThread = new Thread(writer);

		// Armazena as instancias dos gerenciadores de leitura e escrita, e
		// suas threads
		this.readerPair = new Pair<Reader, Thread>(reader, readerThread);
		this.writerPair = new Pair<Writer, Thread>(writer, writerThread);

		// Inicia a exeucão das threads de leitura e escrita
		readerThread.start();
		writerThread.start();
	}

	public String getIP() {
		return serverIP;
	}

	public Reader getReader() {
		return readerPair.getFirst();
	}

	public Writer getWriter() {
		return writerPair.getFirst();
	}

	public Tuple<Integer, String, InetSocketAddress> getUser() {
		return new Tuple<Integer, String, InetSocketAddress>(userID, IDToNameAddress.get(userID).getFirst(), userAddress);
	}

	public HashMap<Integer, Pair<String, InetSocketAddress>> getIDToNameAddress() {
		return IDToNameAddress;
	}

	public String getName(int ID) {
		return IDToNameAddress.get(ID).getFirst();
	}

	public InetSocketAddress getAddress(int ID) {
		return IDToNameAddress.get(ID).getSecond();
	}

	public HashMap<String, Integer> getNameToID() {
		return nameToID;
	}

	public HashMap<InetSocketAddress, Integer> getAddressToID() {
		return addressToID;
	}

	public HashMap<String, List<DisplayMessage>> getGroupMessages() {
		return groupMessages;
	}

	public List<DisplayMessage> getMessages(String groupName) {
		return groupMessages.get(groupName);
	}

	public String getLastMessage(String groupName) {
		if (getMessages(groupName) == null) {
			return "";
		}

		DisplayMessage message = getMessages(groupName).get(getMessages(groupName).size() - 1);
		return IDToNameAddress.get(message.getSenderID()).getFirst() + ": " + message.getMessage();
	}

	public Listener getListener() {
		return listener;
	}

	public HashMap<String, Group> getGroups() {
		return groups;
	}

	public Group getGroup(String name) {
		return groups.get(name);
	}

	public void assignUsername(int ID, String username) {
		userID = ID;

		IDToNameAddress.put(ID, new Pair<String, InetSocketAddress>(username, userAddress));

		nameToID.put(username, ID);
		addressToID.put(userAddress, ID);

		getWriter().queueAction(REQUEST_USER_LIST, null);
	}

	@SuppressWarnings("unchecked")
	public void callEvent(ServerAction action, Object object) {
		switch (action) {
		case VERIFY_USERNAME:
			listener.onVerifyUsername((Integer) object);
			break;
		case USER_CONNECTED:
			listener.onUserConnect((Tuple<Integer, String, InetSocketAddress>) object);
			break;
		case USERS_LIST_UPDATE:
			listener.onUserListUpdate((HashMap<Integer, Pair<String, InetSocketAddress>>) object);
			break;
		case SEND_GROUP:
			listener.onGroupReceive((Group) object);
			break;
		case GROUP_ADD_MEMBER:
			listener.onGroupAddMember((Pair<String, Integer>) object);
			break;
		case GROUP_MESSAGE:
			listener.onGroupMessage((Tuple<String, Integer, byte[]>) object);
			break;
		}
	}

	/**
	 * Avisa à thread de leitura que a conexão do socket foi encerrada
	 */
	public void serverUnnavailble() {
		readerPair.getSecond().interrupt(); // Interrompe a thread de leitura
											// (apenas segurança, thread já deve
											// estar parada nesse ponto)
		writerPair.getFirst().forceStop();  // Força o encerramento da thread de
											// escrita

		System.out.println("FECHOUS");
	}

	public void tryReconnect() {
		ReconnectionThread thread = new ReconnectionThread();
		thread.start();
	}

}
