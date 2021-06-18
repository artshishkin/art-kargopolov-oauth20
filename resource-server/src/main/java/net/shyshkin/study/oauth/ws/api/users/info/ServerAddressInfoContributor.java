package net.shyshkin.study.oauth.ws.api.users.info;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ServerAddressInfoContributor implements InfoContributor {

    private final Environment environment;

    @Override
    public void contribute(Info.Builder builder) {
        String port = environment.getProperty("local.server.port", "UNKNOWN");
        String hostAddress = "UNKNOWN";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
        }

        var serverDetails = Map.of(
                "host", hostAddress,
                "port", port
        );
        builder.withDetail("server", serverDetails);
    }
}
