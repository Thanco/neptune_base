// package host.thanco.NeptuneBase;

// import java.math.BigInteger;
// import java.nio.charset.StandardCharsets;
// import java.security.InvalidAlgorithmParameterException;
// import java.security.InvalidKeyException;
// import java.security.KeyFactory;
// import java.security.NoSuchAlgorithmException;
// import java.security.PublicKey;
// import java.security.spec.InvalidKeySpecException;
// import java.security.spec.MGF1ParameterSpec;
// import java.security.spec.RSAPublicKeySpec;
// import java.util.Base64;
// import java.util.Hashtable;

// import javax.crypto.BadPaddingException;
// import javax.crypto.Cipher;
// import javax.crypto.IllegalBlockSizeException;
// import javax.crypto.KeyGenerator;
// import javax.crypto.NoSuchPaddingException;
// import javax.crypto.SecretKey;
// import javax.crypto.spec.OAEPParameterSpec;
// import javax.crypto.spec.PSource;
// import javax.crypto.spec.SecretKeySpec;

// import com.corundumstudio.socketio.AckCallback;
// import com.corundumstudio.socketio.BroadcastAckCallback;
// import com.corundumstudio.socketio.SocketIOClient;
// import com.google.gson.Gson;

// public class EncryptionHandler {
//     private Hashtable<SocketIOClient, String> clientSessionKeys;

//     public EncryptionHandler() {
//         clientSessionKeys = new Hashtable<>();
//     }

//     public String generateNewClientEncryption(SocketIOClient client, String publicKeyElements) {
//         try {
//             String[] publicKeyArray = new Gson().fromJson(publicKeyElements, String[].class);
//             PublicKey rsaPublicKey = rebuildPublicRSAKey(publicKeyArray[0], Integer.parseInt(publicKeyArray[1]));
//             String encodedSessionKey = encodeSecretKey(generateAESKey());
//             clientSessionKeys.put(client, encodedSessionKey);
//             return publicKeyEncrypt(rsaPublicKey, encodedSessionKey);
//         } catch (Exception e) {
//             e.printStackTrace();
//             return "";
//         }
//     }

//     private PublicKey rebuildPublicRSAKey(String modulusBase64, int exponentInt) throws NoSuchAlgorithmException, InvalidKeySpecException {
//         byte[] modulusBytes = Base64.getDecoder().decode(modulusBase64);
//         BigInteger modulus = new BigInteger(1, modulusBytes);

//         String exponentBinaryString = Integer.toBinaryString(exponentInt);
//         long exponentLong = Long.parseLong(exponentBinaryString);
//         BigInteger exponent = BigInteger.valueOf(exponentLong);

//         // Create the RSAPublicKeySpec with the modulus and exponent
//         RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);

//         // Generate the RSA public key
//         KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//         PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

//         return publicKey;
//     }

//     private String publicKeyEncrypt(PublicKey publicKey, String plaintext) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
//         Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-1ANDMGF1PADDING");
//         OAEPParameterSpec oaepParameterSpecJCE = new OAEPParameterSpec("SHA1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
        
        
//         cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParameterSpecJCE);

//         // byte[] unencryptedBytes = Base64.getDecoder().decode(plaintext);
//         byte[] unencryptedBytes = plaintext.getBytes();
//         System.out.println(unencryptedBytes);

//         byte[] encryptedBytes = cipher.doFinal(unencryptedBytes);
//         String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);

//         return encryptedText;
//     }

//     private SecretKey generateAESKey() throws NoSuchAlgorithmException {
//         KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
//         keyGenerator.init(256); // Key size of 256 bits

//         return keyGenerator.generateKey();
//     }

//     private String encodeSecretKey(SecretKey key) {
//         byte[] keyBytes = key.getEncoded();

//         String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
//         System.out.println(encodedKey);
//         return encodedKey;
//     }

//     public String AESEncryptData(SocketIOClient client, String plaintext) throws Exception {
//         byte[] aesKeyBytes = Base64.getDecoder().decode(clientSessionKeys.get(client));

//         SecretKeySpec secretKey = new SecretKeySpec(aesKeyBytes, "AES");
//         Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//         cipher.init(Cipher.ENCRYPT_MODE, secretKey);

//         byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
//         String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);

//         return encryptedText;
//     }

//     public String AESDecryptData(SocketIOClient client, String encryptedBase64) throws Exception {
//         byte[] aesKeyBytes = Base64.getDecoder().decode(clientSessionKeys.get(client));

//         SecretKeySpec secretKey = new SecretKeySpec(aesKeyBytes, "AES");
//         Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//         cipher.init(Cipher.DECRYPT_MODE, secretKey);

//         byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
//         byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

//         String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);
//         return decryptedText;
//     }

//     public void sendToClient(SocketIOClient client, String type, String message) {
//         sendToClient(client, type, message, false);
//     }

//     public void sendToClient(SocketIOClient client, String type, String message, boolean withAck) {
//         try {
//             if (!withAck) {
//                 client.sendEvent(type, AESEncryptData(client, message));
//                 return;
//             }
//             client.sendEvent(type, new BroadcastAckCallback<>(Character.class, 30) {
//                 protected void onAllSuccess() {
//                     System.out.println("All clients successfully recieved image");
//                 };
//                 protected void onClientSuccess(SocketIOClient client, Character result) {
//                     System.out.println(client.getSessionId() + " recived image.");
//                 };
//                 protected void onClientTimeout(SocketIOClient client) {
//                     // ui.printMessage(new ChatItem(-1, "System", "none", 't', userHandler.getClientUsername(client) + " Failed to AckImage, resending..."));
//                     try {
//                         sendToClient(client, type, message, withAck);
//                     client.sendEvent("image", new AckCallback<>(Character.class, 30) {
//                         public void onSuccess(Character arg0) {
//                             System.out.println(new ChatItem(-1, "System", "none", 't', client.getSessionId() + "recived image resend."));
//                         };
//                         public void onTimeout() {
//                             System.out.println(new ChatItem(-1, "System", "none", 't', client.getSessionId() + "failed to ack image resend."));
//                         };
//                     }, AESEncryptData(client, message));
//                     } catch (Exception e) {
//                         e.printStackTrace();
//                     }
//                 }
//             }, AESEncryptData(client, message));
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     public void clientDisconnect(SocketIOClient client) {
//         clientSessionKeys.remove(client);
//     }
// }
