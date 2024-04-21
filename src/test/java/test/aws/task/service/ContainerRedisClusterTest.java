package test.aws.task.service;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

@Testcontainers
public class ContainerRedisClusterTest {

    private static final Network network = Network.newNetwork();
    public static GenericContainer<?> redis1;
    public static GenericContainer<?> redis2;
    public static GenericContainer<?> redis3;
    public  static RedissonClient redissonClient;
    public static final int REDIS_PORT_1 = 7010;
    public static final int REDIS_PORT_2 = 7011;
    public static final int REDIS_PORT_3 = 7012;
    public static  int mappedPort1;
    public static  int mappedPort2;
    public static  int mappedPort3;

  static {
      redis1 = new GenericContainer<>("redis:latest")
              .withExposedPorts(REDIS_PORT_1)
              .withNetwork(network)
              .withNetworkAliases("redis1")
              .withCommand("redis-server --port "+REDIS_PORT_1 +" --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --appendonly yes");
      redis2 = new GenericContainer<>("redis:latest")
              .withExposedPorts(REDIS_PORT_2)
              .withNetwork(network)
              .withNetworkAliases("redis2")
              .withCommand("redis-server --port "+REDIS_PORT_2 + " --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --appendonly yes");
      redis3 = new GenericContainer<>("redis:latest")
              .withExposedPorts(REDIS_PORT_3)
              .withNetwork(network)
              .withNetworkAliases("redis3")
              .withCommand("redis-server --port "+ REDIS_PORT_3+ " --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --appendonly yes");
      redis1.start();
      redis2.start();
      redis3.start();
      try {
          setupRedisCluster();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
  }

    public static void setupRedisCluster() throws Exception {
        mappedPort1 = redis1.getMappedPort(REDIS_PORT_1);
        mappedPort2 = redis2.getMappedPort(REDIS_PORT_2);
        mappedPort3 = redis3.getMappedPort(REDIS_PORT_3);
        Thread.sleep(3000);
        String command = "redis-cli --cluster create  $(hostname -i):" +mappedPort1+" $(hostname -i):"+ mappedPort2+"  $(hostname -i):"+ mappedPort3;
        redis1.execInContainer("/bin/sh", "-c", command);

        Config config = new Config();
        List<String>  nodeAddresses = Arrays.asList("redis://127.0.0.1:"+mappedPort1,"redis://127.0.0.1:"+mappedPort2, "redis://127.0.0.1:"+mappedPort3);
        config.useClusterServers()
                .addNodeAddress(nodeAddresses.toArray(new String[0]));
        Thread.sleep(3000);
        redissonClient = Redisson.create(config);
  }


}