package net.shyshkin.study.oauth.test.common;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestcontainersUtil {

    public static Network createReusableNetwork(String networkName) {
        Network network = Network.newNetwork();
        try {
            Field nameField = Network.NetworkImpl.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(network, networkName);
            List<com.github.dockerjava.api.model.Network> networks =
                    DockerClientFactory.instance().client().listNetworksCmd().withNameFilter(networkName).exec();
            if (!networks.isEmpty()) {
                Field idField = Network.NetworkImpl.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(network, networks.get(0).getId());
                Field initializedField = Network.NetworkImpl.class.getDeclaredField("initialized");
                initializedField.setAccessible(true);
                ((AtomicBoolean) initializedField.get(network)).set(true);
            }
            return network;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
