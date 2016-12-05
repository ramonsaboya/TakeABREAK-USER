package br.ufpe.cin.if678.communication;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import br.ufpe.cin.if678.Encryption;
import br.ufpe.cin.if678.UserController;
import br.ufpe.cin.if678.business.Group;
import br.ufpe.cin.if678.gui.frame.TakeABREAK;
import br.ufpe.cin.if678.util.Pair;
import br.ufpe.cin.if678.util.Tuple;

public class Listener {

	private UserController controller;

	public Listener(UserController controller) {
		this.controller = controller;
	}

	public void onUserListUpdate(HashMap<InetSocketAddress, String> data) {
		controller.getAddressToName().clear();
		controller.getNameToAddress().clear();

		for (Map.Entry<InetSocketAddress, String> entry : data.entrySet()) {
			controller.getAddressToName().put(entry.getKey(), entry.getValue());
			controller.getNameToAddress().put(entry.getValue(), entry.getKey());
		}

		TakeABREAK.getInstance().getUserListPanel().updateUsers();
	}

	public void onUserConnect(Pair<InetSocketAddress, String> data) {
		controller.getAddressToName().put(data.getFirst(), data.getSecond());
		controller.getNameToAddress().put(data.getSecond(), data.getFirst());

		TakeABREAK.getInstance().getUserListPanel().updateUsers();
	}

	public void onGroupReceive(Group group) {
		controller.getGroups().put(group.getName(), group);

		TakeABREAK.getInstance().notifyGroup(group.getName());

		TakeABREAK.getInstance().getChatListPanel().updateChatList();
	}

	public void onGroupAddMember(Pair<String, InetSocketAddress> data) {
		String name = data.getFirst();
		InetSocketAddress user = data.getSecond();

		controller.getGroup(name).addMember(user);

		TakeABREAK.getInstance().getChatListPanel().updateChatList();
	}

	public void onGroupMessage(Tuple<String, InetSocketAddress, Object> data) {
		String groupName = data.getFirst();
		InetSocketAddress sender = data.getSecond();
		byte[] encryted = (byte[]) data.getThird();

		String message = "";
		try {
			message = Encryption.decryptMessage(sender.getPort(), encryted);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
				| InvalidAlgorithmParameterException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		if (controller.getGroup(groupName) == null) {
			controller.getWriter().queueAction(UserAction.GROUP_CREATE, new Pair<InetSocketAddress, String>(UserController.getInstance().getUser(), groupName));
		}

		TakeABREAK.getInstance().getChatPanel().receiveMessage(groupName, sender, message);
	}

}