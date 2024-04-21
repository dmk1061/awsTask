package test.aws.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;
import java.util.List;
//https://redis.io/learn/operate/redis-at-scale/talking-to-redis/configuring-a-redis-server
//https://github.com/testcontainers/testcontainers-java/issues/3467
@Configuration
public class ConfigRedis {
    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes;
    @Value("${redisson.cluster-servers-config.node-addresses}")
    private List<String> nodeAddresses;
    @Bean
    public RedissonClient redissonClient() {
        final Config config = new Config();
        config.useClusterServers()
                .addNodeAddress(nodeAddresses.toArray(new String[0]));
        return Redisson.create(config);
    }

    @Bean
    final LettuceConnectionFactory redisConnectionFactory(RedisClusterConfiguration redisConfiguration) {
        final ClusterClientOptions clusterClientOptions =
                ClusterClientOptions.builder()
    .nodeFilter(it ->
                ! (it.is(RedisClusterNode.NodeFlag.FAIL)
                        || it.is(RedisClusterNode.NodeFlag.EVENTUAL_FAIL)
                        || it.is(RedisClusterNode.NodeFlag.HANDSHAKE)
                        || it.is(RedisClusterNode.NodeFlag.NOADDR)))
                .validateClusterNodeMembership(false)
                .build();


        final LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clusterClientOptions)
                .readFrom(ReadFrom.MASTER_PREFERRED)
                .commandTimeout(Duration.ofSeconds(120))
                .build();
        final LettuceConnectionFactory lcf =new LettuceConnectionFactory(redisConfiguration, clientConfig);
        lcf.afterPropertiesSet();
        return lcf;
    }

    @Bean
    RedisClusterConfiguration redisConfiguration() {
        final RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(clusterNodes);
        redisClusterConfiguration.setMaxRedirects(5);

        return redisClusterConfiguration;
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate(final RedisConnectionFactory connectionFactory) {
        final RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new JdkSerializationRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        return template;
    }
    @Bean
    public ObjectMapper objectMapper (){
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }


}
