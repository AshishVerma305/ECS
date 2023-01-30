package com.example.AWSECS.Service.impl;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.*;
import com.example.AWSECS.Model.ECSLoadBalancerRequest;
import com.example.AWSECS.Model.ECSRequest;
import com.example.AWSECS.Model.ECSResponse;
import com.example.AWSECS.Model.ECSTargetGroupRequest;
import com.example.AWSECS.Service.ECSService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service("ECSService")
public class ECSServiceImpl implements ECSService {
    @Override
    public ECSResponse startECSChaos(ECSRequest ecsRequest) {
        ECSResponse ecsResponse=new ECSResponse();
        try {
            ECSTargetGroupRequest ecsTargetGroupRequest=ecsRequest.getEcsTargetGroupRequest();
            ECSLoadBalancerRequest ecsLoadBalancerRequest=ecsRequest.getEcsLoadBalancerRequest();
            AmazonElasticLoadBalancing amazonElasticLoadBalancing= AmazonElasticLoadBalancingClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
            if(ecsLoadBalancerRequest.getActionType().equalsIgnoreCase("forward"))
            {
                Matcher matcher=new Matcher();
                matcher.setHttpCode(ecsTargetGroupRequest.getStatusCode());
                CreateTargetGroupRequest createTargetGroupRequest = new CreateTargetGroupRequest();
                createTargetGroupRequest.setName(ecsTargetGroupRequest.getTargetGroupName());
                createTargetGroupRequest.setProtocol(ecsTargetGroupRequest.getProtocol());
                createTargetGroupRequest.setPort(ecsTargetGroupRequest.getPort());
                createTargetGroupRequest.setTargetType(ecsTargetGroupRequest.getTargetType());
                createTargetGroupRequest.setVpcId(ecsTargetGroupRequest.getVpcId());
                createTargetGroupRequest.setHealthCheckIntervalSeconds(30);
                createTargetGroupRequest.setHealthCheckTimeoutSeconds(5);
                createTargetGroupRequest.setUnhealthyThresholdCount(2);
                createTargetGroupRequest.setHealthyThresholdCount(5);
                createTargetGroupRequest.setHealthCheckPath(ecsTargetGroupRequest.getHealthCheckPath());
                createTargetGroupRequest.setIpAddressType(ecsTargetGroupRequest.getIpAddressType());
                createTargetGroupRequest.setMatcher(matcher);
                createTargetGroupRequest.setHealthCheckPort(ecsTargetGroupRequest.getPort().toString());

                CreateTargetGroupResult createTargetGroupResult=amazonElasticLoadBalancing.createTargetGroup(createTargetGroupRequest);
                System.out.println("createTargetGroupResult------------>"+createTargetGroupResult);

                TargetDescription targetDescription=new TargetDescription();
                targetDescription.setId(ecsTargetGroupRequest.getTargetDescriptionId());
                Collection<TargetDescription> targetDescriptionCollection=new ArrayList<>();
                targetDescriptionCollection.add(targetDescription);


                RegisterTargetsRequest registerTargetsRequest=new RegisterTargetsRequest();
                registerTargetsRequest.setTargetGroupArn(createTargetGroupResult.getTargetGroups().get(0).getTargetGroupArn());

                registerTargetsRequest.setTargets(targetDescriptionCollection);
                System.out.println("registerTargetsRequest----------->"+registerTargetsRequest);
                RegisterTargetsResult registerTargetsResult=amazonElasticLoadBalancing.registerTargets(registerTargetsRequest);
                System.out.println("registerTargetsResult----------->"+registerTargetsResult);

                Collection<String> loadBalancerName=new ArrayList<>();
                loadBalancerName.add(ecsLoadBalancerRequest.getLoadBalancerName());

                DescribeLoadBalancersRequest describeLoadBalancersRequest=new DescribeLoadBalancersRequest();
                describeLoadBalancersRequest.setNames(loadBalancerName);
                System.out.println("DescribeLoadBalancersRequest----------->"+describeLoadBalancersRequest);
                DescribeLoadBalancersResult describeLoadBalancersResult=amazonElasticLoadBalancing.describeLoadBalancers(describeLoadBalancersRequest);
                System.out.println("describeLoadBalancersResult------------->"+describeLoadBalancersResult);

                Certificate certificate=new Certificate();
                certificate.setCertificateArn(ecsLoadBalancerRequest.getCertificateArn());
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
                createListenerRequest.setProtocol(ecsLoadBalancerRequest.getProtocol());
                createListenerRequest.setPort(ecsLoadBalancerRequest.getPort());
                createListenerRequest.setCertificates(certificateCollection);
                createListenerRequest.setDefaultActions(actionCollection);
                System.out.println("createListenerRequest-------->"+createListenerRequest);
                CreateListenerResult createListenerResult=amazonElasticLoadBalancing.createListener(createListenerRequest);
                System.out.println("createListenerResult-------->"+createListenerResult);

                Thread.sleep(5000);

                Action action=new Action();
                ForwardActionConfig forwardActionConfig=new ForwardActionConfig();
                TargetGroupTuple targetGroupTuple=new TargetGroupTuple();
                targetGroupTuple.setTargetGroupArn(createTargetGroupResult.getTargetGroups().get(0).getTargetGroupArn());
                targetGroupTuple.setWeight(100);
                Collection<TargetGroupTuple> targetGroupCollection=new ArrayList();
                targetGroupCollection.add(targetGroupTuple);
                forwardActionConfig.setTargetGroups(targetGroupCollection);
                action.setForwardConfig(forwardActionConfig);
                action.setType(ecsLoadBalancerRequest.getActionType());
                Collection<Action> actionCollectionList=new ArrayList<>();
                actionCollectionList.add(action);

                Collection<RuleCondition> ruleConditionCollection=new ArrayList<>();
                RuleCondition ruleCondition=new RuleCondition();
                PathPatternConditionConfig  pathPatternConditionConfig=new PathPatternConditionConfig();
                Collection<String> values=new ArrayList<>();
                values.add(ecsLoadBalancerRequest.getValues());
                pathPatternConditionConfig.setValues(values);
                ruleCondition.setField(ecsLoadBalancerRequest.getField());
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
                ecsResponse.setStatus("SUCCESS");
                return ecsResponse;
            }
            else {
                Collection<String> loadBalancerName=new ArrayList<>();
                loadBalancerName.add(ecsLoadBalancerRequest.getLoadBalancerName());

                DescribeLoadBalancersRequest describeLoadBalancersRequest=new DescribeLoadBalancersRequest();
                describeLoadBalancersRequest.setNames(loadBalancerName);
                System.out.println("DescribeLoadBalancersRequest----------->"+describeLoadBalancersRequest);
                DescribeLoadBalancersResult describeLoadBalancersResult=amazonElasticLoadBalancing.describeLoadBalancers(describeLoadBalancersRequest);
                System.out.println("describeLoadBalancersResult------------->"+describeLoadBalancersResult);

                Certificate certificate=new Certificate();
                certificate.setCertificateArn(ecsLoadBalancerRequest.getCertificateArn());
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
                createListenerRequest.setProtocol(ecsLoadBalancerRequest.getProtocol());
                createListenerRequest.setPort(ecsLoadBalancerRequest.getPort());
                createListenerRequest.setCertificates(certificateCollection);
                createListenerRequest.setDefaultActions(actionCollection);
                System.out.println("createListenerRequest-------->"+createListenerRequest);
                CreateListenerResult createListenerResult=amazonElasticLoadBalancing.createListener(createListenerRequest);
                System.out.println("createListenerResult-------->"+createListenerResult);

                Thread.sleep(5000);

                Action action=new Action();
                action.setType(ecsLoadBalancerRequest.getActionType());
                RedirectActionConfig redirectActionConfig=new RedirectActionConfig();
                redirectActionConfig.setHost(ecsLoadBalancerRequest.getRedirectHost());
                redirectActionConfig.setPath(ecsLoadBalancerRequest.getRedirectPath());
                redirectActionConfig.setPort(ecsLoadBalancerRequest.getRedirectPort());
                redirectActionConfig.setProtocol(ecsLoadBalancerRequest.getProtocol());
                redirectActionConfig.setStatusCode(ecsLoadBalancerRequest.getRedirectStatusCode());
                action.setRedirectConfig(redirectActionConfig);
                action.setType(ecsLoadBalancerRequest.getActionType());
                Collection<Action> actionCollectionList=new ArrayList<>();
                actionCollectionList.add(action);

                Collection<RuleCondition> ruleConditionCollection=new ArrayList<>();
                RuleCondition ruleCondition=new RuleCondition();
                PathPatternConditionConfig  pathPatternConditionConfig=new PathPatternConditionConfig();
                Collection<String> values=new ArrayList<>();
                values.add(ecsLoadBalancerRequest.getValues());
                pathPatternConditionConfig.setValues(values);
                ruleCondition.setField(ecsLoadBalancerRequest.getField());
                ruleCondition.setPathPatternConfig(pathPatternConditionConfig);
                ruleConditionCollection.add(ruleCondition);

                CreateRuleRequest createRuleRequest=new CreateRuleRequest();
                createRuleRequest.setListenerArn(createListenerResult.getListeners().get(0).getListenerArn());
                createRuleRequest.setPriority(ecsLoadBalancerRequest.getPriority());
                createRuleRequest.setActions(actionCollectionList);
                createRuleRequest.setConditions(ruleConditionCollection);
                System.out.println("createRuleRequest-------->"+createRuleRequest);
                CreateRuleResult createRuleResult=amazonElasticLoadBalancing.createRule(createRuleRequest);
                System.out.println("createRuleResult--------->"+createRuleResult);
                ecsResponse.setStatus("SUCCESS");
                return ecsResponse;
            }

        }
        catch (Exception e)
        {
            ecsResponse.setStatus("FAILURE");
            return ecsResponse;
        }
    }
}
