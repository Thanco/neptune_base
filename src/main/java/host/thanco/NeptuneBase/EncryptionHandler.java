package host.thanco.NeptuneBase;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Hashtable;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.SocketIOClient;
import com.google.gson.Gson;

public class EncryptionHandler {
    private Hashtable<SocketIOClient, byte[]> clientSessionKeys;

    public EncryptionHandler() {
        clientSessionKeys = new Hashtable<>();
    }

    public String generateNewClientEncryption(SocketIOClient client, String publicKeyElements) {
        try {
            String[] publicKeyArray = new Gson().fromJson(publicKeyElements, String[].class);
            // int exponent = Base64.getDecoder().decode(publicKeyArray[1])[0];
            PublicKey rsaPublicKey = rebuildPublicRSAKey(publicKeyArray[0], Integer.parseInt(publicKeyArray[1]));
            byte[] clientSessionKey = generateAESKey();
            String encodedSessionKey = Base64.getEncoder().encodeToString(clientSessionKey);
            clientSessionKeys.put(client, clientSessionKey);
            return publicKeyEncrypt(rsaPublicKey, encodedSessionKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private PublicKey rebuildPublicRSAKey(String modulusBase64, int exponentInt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] modulusBytes = Base64.getDecoder().decode(modulusBase64);
        BigInteger modulus = new BigInteger(1, modulusBytes);

        long exponentLong = exponentInt;
        BigInteger exponent = BigInteger.valueOf(exponentLong);

        // Create the RSAPublicKeySpec with the modulus and exponent
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);

        // Generate the RSA public key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        return publicKey;
    }

    private String publicKeyEncrypt(PublicKey publicKey, String plaintext) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-1ANDMGF1PADDING");
        OAEPParameterSpec oaepParameterSpecJCE = new OAEPParameterSpec("SHA1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
        
        
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParameterSpecJCE);

        // byte[] unencryptedBytes = Base64.getDecoder().decode(plaintext);
        byte[] unencryptedBytes = plaintext.getBytes();

        byte[] encryptedBytes = cipher.doFinal(unencryptedBytes);
        String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);

        return encryptedText;
    }

    private byte[] generateAESKey() throws NoSuchAlgorithmException {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return key;
    }

    private byte[] generateNonce() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] nonce = new byte[12];
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    private String AESEncryptData(SocketIOClient client, String plaintext) throws NoSuchPaddingException, NoSuchAlgorithmException,
    InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] key = clientSessionKeys.get(client);
        byte[] nonce = generateNonce();

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);

        byte[] cipherTextWithTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] cipherText = new byte[(cipherTextWithTag.length - 16)];
        byte[] gcmTag = new byte[16];

        for (int i = 0; i < cipherText.length; i++) {
            cipherText[i] = cipherTextWithTag[i];
        }
        for (int i = 0; i < gcmTag.length; i++) {
            gcmTag[i] = cipherTextWithTag[i + cipherText.length];
        }

        // System.arraycopy(cipherTextWithTag, 0, cipherText, 0, (cipherTextWithTag.length - 16));
        // System.arraycopy(cipherTextWithTag, (cipherTextWithTag.length - 16), gcmTag, 0, 16);

        String nonceBase64 = Base64.getEncoder().encodeToString(nonce);
        String cipherTextBase64 = Base64.getEncoder().encodeToString(cipherText);
        String gcmTagBase64 = Base64.getEncoder().encodeToString(gcmTag);
        return nonceBase64 + "%" + cipherTextBase64 + "%" + gcmTagBase64;
    }

    public String AESDecryptData(SocketIOClient client, String encryptedBase64) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] key = clientSessionKeys.get(client);
        String[] split = encryptedBase64.split("%");

        byte[] nonce = Base64.getDecoder().decode(split[0]);
        byte[] ciphertextWithoutTag = Base64.getDecoder().decode(split[1]);
        byte[] gcmTag = Base64.getDecoder().decode(split[2]);

        byte[] encryptedData = new byte[ciphertextWithoutTag.length + gcmTag.length];
        for (int i = 0; i < ciphertextWithoutTag.length; i++) {
            encryptedData[i] = ciphertextWithoutTag[i];
        }
        for (int i = 0; i < gcmTag.length; i++) {
            encryptedData[i + ciphertextWithoutTag.length]  = gcmTag[i];
        }
        // byte[] encryptedData = concatenateByteArrays(ciphertextWithoutTag, gcmTag);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
        
        return new String(cipher.doFinal(encryptedData));
    }

    public void sendToClient(SocketIOClient client, String type, Object message) {
        sendToClient(client, type, message, false);
    }

    public void sendToClient(SocketIOClient client, String type, Object dataToSend, boolean withAck) {
        final String message;
        if (dataToSend.getClass() == String.class) {
            message = (String) dataToSend;
        } else if (dataToSend.getClass() != null) {
            message = new Gson().toJson(dataToSend);
        } else {
            message = "";
        }
        try {
            String messageCrypto = AESEncryptData(client, message);
            if (!withAck) {
                client.sendEvent(type, messageCrypto);
                return;
            }
            client.sendEvent(type, new AckCallback<>(Character.class, 30) {
                @Override
                public void onSuccess(Character result) {
                    System.out.println(client.getSessionId() + " recived image.");
                };
                @Override
                public void onTimeout() {
                    // ui.printMessage(new ChatItem(-1, "System", "none", 't', userHandler.getClientUsername(client) + " Failed to AckImage, resending..."));
                    try {
                    client.sendEvent(type, new AckCallback<>(Character.class, 30) {
                        public void onSuccess(Character arg0) {
                            System.out.println(new ChatItem(-1, "System", "none", 't', client.getSessionId() + "recived image resend."));
                        };
                        public void onTimeout() {
                            System.out.println(new ChatItem(-1, "System", "none", 't', client.getSessionId() + "failed to ack image resend."));
                        };
                    }, messageCrypto);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, messageCrypto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clientDisconnect(SocketIOClient client) {
        clientSessionKeys.remove(client);
    }
}
