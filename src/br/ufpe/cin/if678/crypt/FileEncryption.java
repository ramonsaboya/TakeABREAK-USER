package br.ufpe.cin.if678.crypt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Guarda o nome do arquivo e o arquivo criptografado, a key e o IV usados na criptografia. Contém também os métodos para criptografar e decriptografar o arquivo.
 * 
 * @author joao
 *
 */
public class FileEncryption {

	private byte[] fileName;
	private byte[] file;
	private SecretKey key;
	private byte[] IV;

	/**
	 * Construtor do objeto do FileEncryption
	 * 
	 * @param fileName Array de bytes contendo nome do arquivo criptografado
	 * @param file array de bytes contendo nome do arquivo criptografado
	 * @param key SecretKey contendo a chave usada para criptografia
	 * @param IV Array de bytes contendo IV usado na criptografia
	 */
	public FileEncryption(byte[] fileName, byte[] file, SecretKey key, byte[] IV) {
		this.file = file;
		this.fileName = fileName;
		this.key = key;
		this.IV = IV;
	}
	
	/**
	 * Retorna o atributo de array de bytes do nome do arquivo criptografado
	 * 
	 * @return o nome do arquivo criptografado em array de bytes
	 */
	public byte[] getFileName() {
		return fileName;
	}
	
	/**
	 * Retorna o atributo de array de bytes do arquivo criptografado
	 * 
	 * @return o arquivo criptografado em array de bytes
	 */
	public byte[] getFile() {
		return file;
	}

	/**
	 * Retorna o atributo SecretKey com a chave usada na criptografia
	 * 
	 * @return chave usada na criptografia
	 */
	public SecretKey getKey() {
		return key;
	}

	/**
	 * Retorna o atributo de array de bytes com o IV usado na criptografia
	 * 
	 * @return o IV usado na criptografia
	 */
	public byte[] getIV() {
		return IV;
	}

	/**
	 * Criptografa o arquivo de entrada, devolve o tipo FileEncryption que contém o arquivo e o nome do arquivo criptografados, a key e IV usados para criptografá-los
	 * 
	 * @param file arquivo que será criptografado
	 * @return objeto que contém o arquivo e o nome do arquivo criptografados, a key e IV usados na criptografia
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 */
	public static FileEncryption encrypt(File file) throws IllegalBlockSizeException, BadPaddingException, IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		// Inicia Cipher para a criptografia AES
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");

		// Gera um IV aleatório
		SecureRandom secureRandom = new SecureRandom();
		byte[] IV = new byte[cipher.getBlockSize()];
		secureRandom.nextBytes(IV);

		// Gera uma chave secreta aleatórioa
		KeyGenerator encriptKey = KeyGenerator.getInstance("AES");
		encriptKey.init(128);
		SecretKey key = encriptKey.generateKey();

		// Inicia cipher para o medo de encriptação, com a SecretKey e IV gerados
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV));

		// Encripta o arquivo, a partir do array de bytes
		byte[] encryptedFile = cipher.doFinal(Files.readAllBytes(file.toPath()));

		// Encripta o nome do arquivo
		byte[] fileName = file.getName().getBytes();
		byte[] encryptedFileName = cipher.doFinal(fileName);

		return new FileEncryption(encryptedFileName, encryptedFile, key, IV);
	}

	/**
	 * Recebe um arquivo encriptado para ser decriptado, que será escrito em um diretório
	 * 
	 * @param encryptedFile arquivo que será decriptado
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws NoSuchPaddingException
	 * @throws IOException
	 */
	public static void decrypt(FileEncryption encryptedFile) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IOException {
		// Inicia cipher para o modo AES, de decriptação, com a SecretKey e IV do FileEncryption recebido
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		cipher.init(Cipher.DECRYPT_MODE, encryptedFile.getKey(), new IvParameterSpec(encryptedFile.getIV()));

		// Decripta o arquivo
		byte[] decryptedFile = cipher.doFinal(encryptedFile.getFile());

		// Decripta o nome do arquivo
		String decryptedFileName = new String (cipher.doFinal(encryptedFile.getFileName()), "UTF-8");
		
		// Escreve o arquivo no Path definido.
		Path writePath = Paths.get("" + decryptedFileName);
		Files.write(writePath, decryptedFile);
	}
}