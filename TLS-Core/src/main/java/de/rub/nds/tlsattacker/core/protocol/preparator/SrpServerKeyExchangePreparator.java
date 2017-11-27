/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol.preparator;

import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.tlsattacker.core.constants.HandshakeByteLength;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.crypto.SignatureCalculator;
import de.rub.nds.tlsattacker.core.protocol.message.SrpServerKeyExchangeMessage;
import static de.rub.nds.tlsattacker.core.protocol.preparator.Preparator.LOGGER;
import de.rub.nds.tlsattacker.core.workflow.chooser.Chooser;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SrpServerKeyExchangePreparator extends ServerKeyExchangePreparator<SrpServerKeyExchangeMessage> {

    private BigInteger publicKey;
    private SignatureAndHashAlgorithm selectedSignatureHashAlgo;
    private byte[] signature;
    private final SrpServerKeyExchangeMessage msg;

    public SrpServerKeyExchangePreparator(Chooser chooser, SrpServerKeyExchangeMessage message) {
        super(chooser, message);
        this.msg = message;
    }

    @Override
    public void prepareHandshakeMessageContents() {
        msg.prepareComputations();
        setComputedModulus(msg);
        setComputedGenerator(msg);
        setComputedSalt(msg);
        setComputedPrivateKey(msg);
        setSRPIdentity(msg);
        setSRPPassword(msg);
        BigInteger modulus = msg.getComputations().getModulus().getValue();
        BigInteger generator = msg.getComputations().getGenerator().getValue();
        BigInteger privateKey = msg.getComputations().getPrivateKey().getValue();
        byte[] identity = msg.getComputations().getSRPIdentity().getValue();
        byte[] password = msg.getComputations().getSRPPassword().getValue();
        byte[] salt = msg.getComputations().getSalt().getValue();

        // Compute PublicKey
        publicKey = generatePublicKey(modulus, generator, privateKey, identity, password, salt);
        publicKey.mod(modulus);
        prepareModulus(msg);
        prepareModulusLength(msg);
        prepareGenerator(msg);
        prepareGeneratorLength(msg);
        prepareSalt(msg);
        prepareSaltLength(msg);
        preparePublicKey(msg);
        preparePublicKeyLength(msg);
        selectedSignatureHashAlgo = chooser.getSelectedSigHashAlgorithm();
        prepareSignatureAndHashAlgorithm(msg);
        prepareClientRandom(msg);
        prepareServerRandom(msg);
        signature = generateSignature(selectedSignatureHashAlgo);
        prepareSignature(msg);
        prepareSignatureLength(msg);
    }

    private BigInteger generatePublicKey(BigInteger modulus, BigInteger generator, BigInteger privateKey,
            byte[] identity, byte[] password, byte[] salt) {
        BigInteger publickey;
        BigInteger k = calculateSRP6Multiplier(modulus, generator);
        BigInteger x = calculateX(salt, identity, password);
        BigInteger v = generator.modPow(x, modulus);
        BigInteger helpValue1 = generator.modPow(privateKey, modulus);
        BigInteger helpValue2 = k.multiply(v);
        helpValue2.mod(modulus);
        helpValue1 = helpValue2.add(helpValue1);
        publickey = helpValue1.mod(modulus);
        LOGGER.debug(ArrayConverter.bytesToHexString(ArrayConverter.bigIntegerToByteArray(helpValue1)));
        LOGGER.debug(ArrayConverter.bytesToHexString(ArrayConverter.bigIntegerToByteArray(publickey)));
        return publickey;
    }

    public BigInteger calculateX(byte[] salt, byte[] identity, byte[] password) {
        byte[] hashInput1 = ArrayConverter.concatenate(identity, ArrayConverter.hexStringToByteArray("3A"), password);
        LOGGER.debug("HashInput for hashInput1: " + ArrayConverter.bytesToHexString(hashInput1));
        byte[] hashOutput1 = SHAsum(hashInput1);
        LOGGER.debug("Hashvalue for hashInput1: " + ArrayConverter.bytesToHexString(hashOutput1));
        byte[] hashInput2 = ArrayConverter.concatenate(salt, hashOutput1);
        LOGGER.debug("HashInput for hashInput2: " + ArrayConverter.bytesToHexString(hashInput2));
        byte[] hashOutput2 = SHAsum(hashInput2);
        LOGGER.debug("Hashvalue for hashInput2: " + ArrayConverter.bytesToHexString(hashOutput2));
        BigInteger output = new BigInteger(1, hashOutput2);
        return output;
    }

    private BigInteger calculateSRP6Multiplier(BigInteger modulus, BigInteger generator) {
        BigInteger srp6Multiplier;
        byte[] paddedGenerator = calculatePadding(modulus, generator);
        byte[] hashInput = ArrayConverter.concatenate(ArrayConverter.bigIntegerToByteArray(modulus), paddedGenerator);
        LOGGER.debug("HashInput SRP6Multi: " + ArrayConverter.bytesToHexString(hashInput));
        byte[] hashOutput = SHAsum(hashInput);
        srp6Multiplier = new BigInteger(1, hashOutput);
        return srp6Multiplier;
    }

    public byte[] SHAsum(byte[] toHash) {
        MessageDigest dig = null;
        try {
            dig = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        dig.update(toHash);
        byte[] hashword = dig.digest();
        return hashword;
    }

    private byte[] calculatePadding(BigInteger modulus, BigInteger topad) {
        byte[] padding;
        int modulusByteLength = ArrayConverter.bigIntegerToByteArray(modulus).length;
        byte[] paddingArray = ArrayConverter.bigIntegerToByteArray(topad);
        if (modulusByteLength == paddingArray.length) {
            return paddingArray;
        }
        int paddingByteLength = modulusByteLength - paddingArray.length;
        padding = new byte[paddingByteLength];
        byte[] output = ArrayConverter.concatenate(padding, paddingArray);
        return output;
    }

    private byte[] generateToBeSigned() {
        byte[] srpParams = ArrayConverter.concatenate(ArrayConverter.intToBytes(msg.getModulusLength().getValue(),
                HandshakeByteLength.SRP_MODULUS_LENGTH), msg.getModulus().getValue(), ArrayConverter.intToBytes(msg
                .getGeneratorLength().getValue(), HandshakeByteLength.SRP_GENERATOR_LENGTH), msg.getGenerator()
                .getValue(), ArrayConverter.intToBytes(msg.getSaltLength().getValue(),
                HandshakeByteLength.SRP_SALT_LENGTH), msg.getSalt().getValue(), ArrayConverter.intToBytes(msg
                .getPublicKeyLength().getValue(), HandshakeByteLength.SRP_PUBLICKEY_LENGTH), msg.getPublicKey()
                .getValue());
        return ArrayConverter.concatenate(msg.getComputations().getClientRandom().getValue(), msg.getComputations()
                .getServerRandom().getValue(), srpParams);

    }

    private byte[] generateSignature(SignatureAndHashAlgorithm algorithm) {
        return SignatureCalculator.generateSignature(algorithm, chooser, generateToBeSigned());
    }

    private void prepareGenerator(SrpServerKeyExchangeMessage msg) {
        msg.setGenerator(msg.getComputations().getGenerator().getByteArray());
        LOGGER.debug("Generator: " + ArrayConverter.bytesToHexString(msg.getGenerator().getValue()));
    }

    private void prepareModulus(SrpServerKeyExchangeMessage msg) {
        msg.setModulus(msg.getComputations().getModulus().getByteArray());
        LOGGER.debug("Modulus: " + ArrayConverter.bytesToHexString(msg.getModulus().getValue()));
    }

    private void prepareGeneratorLength(SrpServerKeyExchangeMessage msg) {
        msg.setGeneratorLength(msg.getGenerator().getValue().length);
        LOGGER.debug("Generator Length: " + msg.getGeneratorLength().getValue());
    }

    private void prepareSalt(SrpServerKeyExchangeMessage msg) {
        msg.setSalt(msg.getComputations().getSalt());
        LOGGER.debug("Salt: " + ArrayConverter.bytesToHexString(msg.getSalt().getValue()));
    }

    private void prepareSaltLength(SrpServerKeyExchangeMessage msg) {
        msg.setSaltLength(msg.getSalt().getValue().length);
        LOGGER.debug("Salt Length: " + msg.getSaltLength().getValue());
    }

    private void prepareModulusLength(SrpServerKeyExchangeMessage msg) {
        msg.setModulusLength(msg.getModulus().getValue().length);
        LOGGER.debug("Modulus Length: " + msg.getModulusLength().getValue());
    }

    private void preparePublicKey(SrpServerKeyExchangeMessage msg) {
        msg.setPublicKey(publicKey.toByteArray());
        LOGGER.debug("PublicKey: " + ArrayConverter.bytesToHexString(msg.getPublicKey().getValue()));
    }

    private void preparePublicKeyLength(SrpServerKeyExchangeMessage msg) {
        msg.setPublicKeyLength(msg.getPublicKey().getValue().length);
        LOGGER.debug("PublicKeyLength: " + msg.getPublicKeyLength().getValue());
    }

    private void setComputedPrivateKey(SrpServerKeyExchangeMessage msg) {
        msg.getComputations().setPrivateKey(chooser.getSRPServerPrivateKey());
        LOGGER.debug("PrivateKey: " + msg.getComputations().getPrivateKey().getValue());
    }

    private void setComputedModulus(SrpServerKeyExchangeMessage msg) {
        msg.getComputations().setModulus(chooser.getSRPModulus());
        LOGGER.debug("Modulus used for Computations: " + msg.getComputations().getModulus().getValue().toString(16));
    }

    private void setSRPIdentity(SrpServerKeyExchangeMessage msg) {
        msg.getComputations().setSRPIdentity(chooser.getSRPIdentity());
        LOGGER.debug("SRP Identity used for Computations: " + msg.getComputations().getSRPIdentity());
    }

    private void setSRPPassword(SrpServerKeyExchangeMessage msg) {
        msg.getComputations().setSRPPassword(chooser.getSRPPassword());
        LOGGER.debug("SRP Password used for Computations: " + msg.getComputations().getSRPPassword());
    }

    private void setComputedSalt(SrpServerKeyExchangeMessage msg) {
        msg.getComputations().setSalt(chooser.getSRPServerSalt());
        LOGGER.debug("Salt used for Computations: " + msg.getComputations().getSalt());
    }

    private void setComputedGenerator(SrpServerKeyExchangeMessage msg) {
        msg.getComputations().setGenerator(chooser.getSRPGenerator());
        LOGGER.debug("Generator used for Computations: " + msg.getComputations().getGenerator().getValue().toString(16));
    }

    private void prepareSignatureAndHashAlgorithm(SrpServerKeyExchangeMessage msg) {
        msg.setSignatureAndHashAlgorithm(selectedSignatureHashAlgo.getByteValue());
        LOGGER.debug("SignatureAlgorithm: "
                + ArrayConverter.bytesToHexString(msg.getSignatureAndHashAlgorithm().getValue()));
    }

    private void prepareClientRandom(SrpServerKeyExchangeMessage msg) {
        msg.getComputations().setClientRandom(chooser.getClientRandom());
        LOGGER.debug("ClientRandom: "
                + ArrayConverter.bytesToHexString(msg.getComputations().getClientRandom().getValue()));
    }

    private void prepareServerRandom(SrpServerKeyExchangeMessage msg) {
        msg.getComputations().setServerRandom(chooser.getServerRandom());
        LOGGER.debug("ServerRandom: "
                + ArrayConverter.bytesToHexString(msg.getComputations().getServerRandom().getValue()));
    }

    private void prepareSignature(SrpServerKeyExchangeMessage msg) {
        msg.setSignature(signature);
        LOGGER.debug("Signatur: " + ArrayConverter.bytesToHexString(msg.getSignature().getValue()));
    }

    private void prepareSignatureLength(SrpServerKeyExchangeMessage msg) {
        msg.setSignatureLength(msg.getSignature().getValue().length);
        LOGGER.debug("SignatureLength: " + msg.getSignatureLength().getValue());
    }
}
