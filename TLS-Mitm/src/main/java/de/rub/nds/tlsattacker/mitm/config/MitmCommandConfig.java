/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package de.rub.nds.tlsattacker.mitm.config;

import com.beust.jcommander.ParametersDelegate;
import de.rub.nds.tlsattacker.core.config.TLSDelegateConfig;
import de.rub.nds.tlsattacker.core.config.delegate.CertificateDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.CiphersuiteDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.EllipticCurveDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.GeneralDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.HeartbeatDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.MaxFragmentLengthDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.MitmDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.MitmWorkflowTypeDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.ProtocolVersionDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.SignatureAndHashAlgorithmDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.TransportHandlerDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.WorkflowInputDelegate;
import de.rub.nds.tlsattacker.core.config.delegate.WorkflowOutputDelegate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Lucas Hartmann <lucas.hartmann@rub.de>
 */

public class MitmCommandConfig extends TLSDelegateConfig {

    protected static final Logger LOGGER = LogManager.getLogger(MitmCommandConfig.class);

    public static final String COMMAND = "mitm";

    @ParametersDelegate
    private final GeneralDelegate generalDelegate;
    @ParametersDelegate
    private final CiphersuiteDelegate ciphersuiteDelegate;
    @ParametersDelegate
    private final ProtocolVersionDelegate protocolVersionDelegate;
    @ParametersDelegate
    private final EllipticCurveDelegate ellipticCurveDelegate;
    @ParametersDelegate
    private final MitmDelegate mitmDelegate;
    @ParametersDelegate
    private final SignatureAndHashAlgorithmDelegate signatureAndHashAlgorithmDelegate;
    @ParametersDelegate
    private final WorkflowInputDelegate workflowInputDelegate;
    @ParametersDelegate
    private final WorkflowOutputDelegate workflowOutputDelegate;
    @ParametersDelegate
    private final MitmWorkflowTypeDelegate mitmWorkflowTypeDelegate;
    @ParametersDelegate
    private final TransportHandlerDelegate transportHandlerDelegate;
    @ParametersDelegate
    private final HeartbeatDelegate heartbeatDelegate;
    @ParametersDelegate
    private final MaxFragmentLengthDelegate maxFragmentLengthDelegate;
    @ParametersDelegate
    private final CertificateDelegate certificateDelegate;

    public MitmCommandConfig(GeneralDelegate delegate) {
        super(delegate);
        this.generalDelegate = delegate;
        this.ciphersuiteDelegate = new CiphersuiteDelegate();
        this.heartbeatDelegate = new HeartbeatDelegate();
        this.ellipticCurveDelegate = new EllipticCurveDelegate();
        this.protocolVersionDelegate = new ProtocolVersionDelegate();
        this.mitmDelegate = new MitmDelegate();
        this.signatureAndHashAlgorithmDelegate = new SignatureAndHashAlgorithmDelegate();
        this.transportHandlerDelegate = new TransportHandlerDelegate();
        this.workflowOutputDelegate = new WorkflowOutputDelegate();
        this.workflowInputDelegate = new WorkflowInputDelegate();
        this.mitmWorkflowTypeDelegate = new MitmWorkflowTypeDelegate();
        this.maxFragmentLengthDelegate = new MaxFragmentLengthDelegate();
        this.certificateDelegate = new CertificateDelegate();
        addDelegate(maxFragmentLengthDelegate);
        addDelegate(ciphersuiteDelegate);
        addDelegate(ellipticCurveDelegate);
        addDelegate(protocolVersionDelegate);
        addDelegate(mitmDelegate);
        addDelegate(signatureAndHashAlgorithmDelegate);
        addDelegate(heartbeatDelegate);
        addDelegate(workflowOutputDelegate);
        addDelegate(transportHandlerDelegate);
        addDelegate(certificateDelegate);
        addDelegate(workflowInputDelegate);
        addDelegate(workflowOutputDelegate);
        addDelegate(mitmWorkflowTypeDelegate);
    }
}