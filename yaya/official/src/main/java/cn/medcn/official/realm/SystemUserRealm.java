package cn.medcn.official.realm;

import cn.medcn.common.utils.MD5Utils;
import cn.medcn.official.model.OffUserInfo;
import cn.medcn.official.security.Principal;
import cn.medcn.official.service.OffiUserInfoService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * Created by huanghuibin on 2017/11/6.
 */
public class SystemUserRealm extends AuthorizingRealm {

    @Autowired
    private OffiUserInfoService offiUserInfoService;

    /**
     * 授权查询回调函数, 进行鉴权但缓存中无用户的授权信息时调用.
     *
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        String account = String.valueOf(principalCollection.getPrimaryPrincipal());
        OffUserInfo user = new OffUserInfo();
        user.setAccount(account);
        final OffUserInfo loginUser = offiUserInfoService.selectOne(user);
        if (loginUser == null) return null;
        return authorizationInfo;
    }

    /**
     * 认证回调函数,登录时调用.
     *
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)  throws AuthenticationException{
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        String password = new String(token.getPassword());
        token.setPassword(MD5Utils.MD5Encode(password).toCharArray());
        // 校验用户名密码
        OffUserInfo condition = new OffUserInfo();
        condition.setUserName(token.getUsername());
        OffUserInfo user = offiUserInfoService.selectOne(condition);
        if (user != null) {
            Principal principal = Principal.build(user);
            SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(principal,
                    user.getPassword(), getName());
            if (!getCredentialsMatcher().doCredentialsMatch(token, info)) {
                throw new AuthenticationException("密码不正确.");
            }
            user.setLastLoginIp(((UsernamePasswordToken) authenticationToken).getHost());
            user.setLastLoginDate(new Date());
            offiUserInfoService.updateByPrimaryKeySelective(user);
            //登录成功
            return info;
        } else {
            throw new AuthenticationException("该帐号不存在.");
        }
    }
}