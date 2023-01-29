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
import com.amazonaws.services.ec2.model.TargetGroup;
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
            System.out.println("createTargetGroupResult------------>"+createTargetGroupResult);

            TargetDescription targetDescription=new TargetDescription();
            targetDescription.setId("10.100.5.43");
            Collection<TargetDescription> targetDescriptionCollection=new ArrayList<>();
            targetDescriptionCollection.add(targetDescription);


            RegisterTargetsRequest registerTargetsRequest=new RegisterTargetsRequest();
            registerTargetsRequest.setTargetGroupArn(createTargetGroupResult.getTargetGroups().get(0).getTargetGroupArn());

            registerTargetsRequest.setTargets(targetDescriptionCollection);
            System.out.println("registerTargetsRequest----------->"+registerTargetsRequest);
            RegisterTargetsResult registerTargetsResult=amazonElasticLoadBalancing.registerTargets(registerTargetsRequest);
            System.out.println("registerTargetsResult----------->"+registerTargetsResult);

            Collection<String> loadBalancerName=new ArrayList<>();
            loadBalancerName.add("cmdev-ondotsystems-com");

            DescribeLoadBalancersRequest describeLoadBalancersRequest=new DescribeLoadBalancersRequest();
            describeLoadBalancersRequest.setNames(loadBalancerName);
            System.out.println("DescribeLoadBalancersRequest----------->"+describeLoadBalancersRequest);
            DescribeLoadBalancersResult describeLoadBalancersResult=amazonElasticLoadBalancing.describeLoadBalancers(describeLoadBalancersRequest);
            System.out.println("describeLoadBalancersResult------------->"+describeLoadBalancersResult);

            Certificate certificate=new Certificate();
            certificate.setCertificateArn("arn:aws:acm:us-east-1:882538723223:certificate/767c8f8b-1ac6-4e48-bf08-0e7af74d3fb4");
            Collection<Certificate> certificateCollection= new ArrayList<>();
            certificateCollection.add(certificate);

            Action defaultAction=new Action();
            FixedResponseActionConfig fixedResponseActionConfig=new FixedResponseActionConfig();
            fixedResponseActionConfig.setContentType("text/plain");
            fixedResponseActionConfig.setMessageBody("Page Not Found");
            fixedResponseActionConfig.setStatusCode("404");
            defaultAction.setFixedResponseConfig(fixedResponseActionConfig);
            defaultAction.setType(ActionTypeEnum.FixedResponse);
            Collection<Action> actionCollection= new ArrayList<>();
            actionCollection.add(defaultAction);

            CreateListenerRequest createListenerRequest=new CreateListenerRequest();
            createListenerRequest.setLoadBalancerArn(describeLoadBalancersResult.getLoadBalancers().get(0).getLoadBalancerArn());
            createListenerRequest.setProtocol(ProtocolEnum.HTTPS);
            createListenerRequest.setPort(8444);
            createListenerRequest.setCertificates(certificateCollection);
            createListenerRequest.setDefaultActions(actionCollection);
            System.out.println("createListenerRequest-------->"+createListenerRequest);
            CreateListenerResult createListenerResult=amazonElasticLoadBalancing.createListener(createListenerRequest);
            System.out.println("createListenerResult-------->"+createListenerResult);



            Action action=new Action();
            ForwardActionConfig forwardActionConfig=new ForwardActionConfig();
            TargetGroupTuple targetGroupTuple=new TargetGroupTuple();
            targetGroupTuple.setTargetGroupArn(createTargetGroupResult.getTargetGroups().get(0).getTargetGroupArn());
            targetGroupTuple.setWeight(100);
            Collection<TargetGroupTuple> targetGroupCollection=new ArrayList();
            targetGroupCollection.add(targetGroupTuple);
            forwardActionConfig.setTargetGroups(targetGroupCollection);
            action.setForwardConfig(forwardActionConfig);
            Collection<Action> actionCollectionList=new ArrayList<>();
            actionCollectionList.add(action);

            Collection<RuleCondition> ruleConditionCollection=new ArrayList<>();
            RuleCondition ruleCondition=new RuleCondition();
            PathPatternConditionConfig  pathPatternConditionConfig=new PathPatternConditionConfig();
            Collection<String> values=new ArrayList<>();
            values.add("/api/*");
            pathPatternConditionConfig.setValues(values);
            ruleCondition.setField("path-pattern");
            ruleCondition.setPathPatternConfig(pathPatternConditionConfig);
            ruleConditionCollection.add(ruleCondition);

            CreateRuleRequest createRuleRequest=new CreateRuleRequest();
            createRuleRequest.setListenerArn(createListenerResult.getListeners().get(0).getListenerArn());
            createRuleRequest.setPriority(1);
            createRuleRequest.setActions(actionCollectionList);
            createRuleRequest.setConditions(ruleConditionCollection);
            System.out.println("createRuleRequest-------->"+createRuleRequest);
            CreateRuleResult createRuleResult=amazonElasticLoadBalancing.createRule(createRuleRequest);
            System.out.println("createRuleResult--------->"+createRuleResult);
        }
        catch (Exception e)
        {

        }

    }
}
