package com.example.AWSECS.Controller;




import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.model.ECSService;
import com.amazonaws.services.dynamodbv2.xspec.M;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2instanceconnect.AWSEC2InstanceConnectClient;
import com.amazonaws.services.ecs.AmazonECS;

import com.amazonaws.services.ecs.AmazonECSClientBuilder;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.*;
import com.amazonaws.services.opsworks.model.ElasticLoadBalancer;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;


@RestController
@CrossOrigin
@RequestMapping("/aws")
public class controller {

    @GetMapping("/chaos")
    public void chaos() {

        try {



            AmazonEC2 amazonEC2 = AmazonEC2ClientBuilder.standard().withRegion(Regions.US_EAST_1)
                    .build();
            String instanceId="i-01617add322847a93";

            Matcher matcher=new Matcher();
            matcher.setHttpCode("404");
            CreateTargetGroupRequest createTargetGroupRequest = new CreateTargetGroupRequest();
            createTargetGroupRequest.setName("chaos-tg-8083");
            createTargetGroupRequest.setProtocol(ProtocolEnum.HTTPS);
            createTargetGroupRequest.setPort(8083);
            createTargetGroupRequest.setTargetType(TargetTypeEnum.Ip);
            createTargetGroupRequest.setVpcId("vpc-06c677ddaf72b471b");
            createTargetGroupRequest.setHealthCheckIntervalSeconds(30);
            createTargetGroupRequest.setHealthCheckTimeoutSeconds(5);
            createTargetGroupRequest.setUnhealthyThresholdCount(2);
            createTargetGroupRequest.setHealthyThresholdCount(5);
            createTargetGroupRequest.setHealthCheckPath("/");
            createTargetGroupRequest.setIpAddressType(TargetGroupIpAddressTypeEnum.Ipv4);
            createTargetGroupRequest.setMatcher(matcher);
            createTargetGroupRequest.setHealthCheckPort("8083");
            AmazonElasticLoadBalancing amazonElasticLoadBalancing=AmazonElasticLoadBalancingClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
            CreateTargetGroupResult createTargetGroupResult=amazonElasticLoadBalancing.createTargetGroup(createTargetGroupRequest);
            System.out.println("createTargetGroupResult"+createTargetGroupResult);

            TargetDescription targetDescription=new TargetDescription();
            targetDescription.setId("10.100.5.43");
            Collection<TargetDescription> targetDescriptionCollection=new ArrayList<>();
            targetDescriptionCollection.add(targetDescription);


            RegisterTargetsRequest registerTargetsRequest=new RegisterTargetsRequest();
            registerTargetsRequest.setTargetGroupArn(createTargetGroupResult.getTargetGroups().get(0).getTargetGroupArn());

            registerTargetsRequest.setTargets(targetDescriptionCollection);
            System.out.println("registerTargetsRequest"+registerTargetsRequest);
            RegisterTargetsResult registerTargetsResult=amazonElasticLoadBalancing.registerTargets(registerTargetsRequest);
            System.out.println("registerTargetsResult"+registerTargetsResult);

            Collection<String> loadBalancerName=new ArrayList<>();
            loadBalancerName.add("cmdev-ondotsystems-com");
            
            DescribeLoadBalancersRequest describeLoadBalancersRequest=new DescribeLoadBalancersRequest();
            describeLoadBalancersRequest.setNames(loadBalancerName);
            DescribeLoadBalancersResult describeLoadBalancersResult=amazonElasticLoadBalancing.describeLoadBalancers(describeLoadBalancersRequest);
            System.out.println("describeLoadBalancersResult"+describeLoadBalancersResult);
        }
        catch (Exception e)
        {

        }

    }
}
