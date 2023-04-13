package api;

import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.qameta.allure.grpc.AllureGrpc;
import sashkir7.grpc.UsernameRequest;

abstract class BaseGrpcApi {

    protected final Channel channel;
    protected final Empty defaultEmptyInstance = Empty.getDefaultInstance();

    public BaseGrpcApi(String address, int port) {
        channel = ManagedChannelBuilder.forAddress(address, port)
                .intercept(new AllureGrpc())
                .usePlaintext()
                .build();
    }

    protected UsernameRequest getUsernameRequest(String username) {
        return UsernameRequest.newBuilder().setUsername(username).build();
    }

}