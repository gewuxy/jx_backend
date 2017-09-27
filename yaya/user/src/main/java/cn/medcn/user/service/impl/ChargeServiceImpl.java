package cn.medcn.user.service.impl;

import cn.medcn.common.service.impl.BaseServiceImpl;
import cn.medcn.user.dao.FluxOrderDAO;
import cn.medcn.user.dao.UserFluxDAO;
import cn.medcn.user.model.FluxOrder;
import cn.medcn.user.model.UserFlux;
import cn.medcn.user.service.ChargeService;
import com.github.abel533.mapper.Mapper;
import com.pingplusplus.exception.*;
import com.pingplusplus.model.Charge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by LiuLP on 2017/9/25.
 */
@Service
public class ChargeServiceImpl extends BaseServiceImpl<FluxOrder> implements ChargeService {


    @Autowired
    protected FluxOrderDAO fluxOrderDAO;

    @Autowired
    protected UserFluxDAO userFluxDAO;

    @Override
    public Mapper<FluxOrder> getBaseMapper() {
        return fluxOrderDAO;
    }



    /**
     * 创建Charge对象，返回给前端
     *
     * @param appId
     * @param amount
     * @param channel
     * @param ip
     * @return
     * @throws RateLimitException
     * @throws APIException
     * @throws ChannelException
     * @throws InvalidRequestException
     * @throws APIConnectionException
     * @throws AuthenticationException
     */
    public Charge createCharge(String orderNo, String appId, Integer amount, String channel, String ip) throws RateLimitException, APIException, ChannelException, InvalidRequestException, APIConnectionException, AuthenticationException {
        Map<String, Object> chargeParams = new HashMap();

        chargeParams.put("order_no", orderNo);
        //单位为对应币种的最小货币单位，人民币为分。如订单总金额为 1 元， amount 为 100
        chargeParams.put("amount", amount * 100);
        Map<String, String> app = new HashMap();
        //appId
        app.put("id", appId);
        chargeParams.put("app", app);
        chargeParams.put("channel", channel);
        chargeParams.put("currency", "cny");
        chargeParams.put("client_ip", ip);
        chargeParams.put("subject", "charge flux");
        chargeParams.put("body", "charge flux");
        return Charge.create(chargeParams);
    }


    /**
     * 创建订单
     *
     * @param userId
     * @param orderNo 订单id
     * @param amount  交易金额
     * @param channel 付款方式
     */
    public void createOrder(String userId, String orderNo, Integer amount, String channel) {
        FluxOrder order = new FluxOrder();
        order.setUserId(userId);
        order.setBuyTime(new Date());
        order.setTradeId(orderNo);
        order.setState(0);
        //将金额转换为流量，再转为以M为单位
        order.setFlux((int) (amount / 2.0 * 1024));
        order.setPlatform(channel);
        fluxOrderDAO.insert(order);
    }





    /**
     * 更新订单状态和用户流量值
     * @param result
     */
    public void updateOrderAndUserFlux(FluxOrder result) {
        //更新订单状态
        result.setState(1);
        result.setEffectTime(new Date());
        fluxOrderDAO.updateByPrimaryKey(result);

        //查找充值记录
        String userId = result.getUserId();
        UserFlux flux = userFluxDAO.selectByPrimaryKey(userId);
        //没有充值记录，创建充值记录
        if(flux == null){
            UserFlux condition = new UserFlux();
            condition.setUserId(userId);
            condition.setFlux(result.getFlux());
            userFluxDAO.insert(condition);
        }else{
            flux.setFlux(flux.getFlux() + result.getFlux());
            userFluxDAO.updateByPrimaryKey(flux);
        }

    }

}