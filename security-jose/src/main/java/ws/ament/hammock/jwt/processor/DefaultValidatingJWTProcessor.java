/*
 * Copyright 2017 Hammock and its contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.ament.hammock.jwt.processor;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.deltaspike.core.util.StringUtils;
import ws.ament.hammock.jwt.JWTConfiguration;
import ws.ament.hammock.jwt.JWTException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

@ApplicationScoped
public class DefaultValidatingJWTProcessor implements JWTProcessor{
    private ConfigurableJWTProcessor<SecurityContext> delegate = new DefaultJWTProcessor<>();

    @Inject
    private JWTConfiguration jwtConfiguration;

    @PostConstruct
    public void init() {
        try {
            JWKSource<SecurityContext> keySource = lookupJWKSource();
            JWSAlgorithm expectedJWSAlg = jwtConfiguration.getAlgorithm();
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
            delegate.setJWSKeySelector(keySelector);
        } catch (IOException | ParseException e) {
            throw new JWTException("Unable to read JWT Configuration",e);
        }
    }

    @Override
    public Map<String, Object> process(String jwt) throws JWTException {
        try {
            return delegate.process(jwt, null).toJSONObject();
        } catch (ParseException | BadJOSEException | JOSEException e) {
            throw new JWTException("Unable to parse jwt", e);
        }
    }

    private JWKSource<SecurityContext> lookupJWKSource() throws IOException, ParseException {
        if(StringUtils.isNotEmpty(jwtConfiguration.getJwkSourceUrl())) {
            return new RemoteJWKSet<>(new URL(jwtConfiguration.getJwkSourceUrl()));
        }
        else {
            JWKSet jwkSet = JWKSet.load(new File(jwtConfiguration.getJwkSourceFile()));

            return new ImmutableJWKSet<>(jwkSet);
        }
    }
}
