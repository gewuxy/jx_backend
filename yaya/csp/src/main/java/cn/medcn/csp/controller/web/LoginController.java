package cn.medcn.csp.controller.web;

import cn.medcn.common.excptions.SystemException;
 import cn.medcn.common.utils.CheckUtils;
import cn.medcn.common.utils.JsonUtils;
import cn.medcn.common.utils.LocalUtils;
import cn.medcn.common.utils.StringUtils;
import cn.medcn.csp.controller.CspBaseController;
import cn.medcn.csp.security.Principal;
import cn.medcn.oauth.dto.OAuthUser;
import cn.medcn.oauth.service.OauthService;
import cn.medcn.sys.service.SysNotifyService;
import cn.medcn.user.dto.Captcha;
import cn.medcn.user.dto.CspUserInfoDTO;
import cn.medcn.user.model.BindInfo;
import cn.medcn.user.model.CspUserInfo;
import cn.medcn.user.model.EmailTemplate;
import cn.medcn.user.service.CspUserService;
import cn.medcn.user.service.EmailTempService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
 import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;

/**
 * Created by lixuan on 2017/10/16.
 */
@Controller
@RequestMapping(value = "/mgr")
public class LoginController extends CspBaseController {

    @Autowired
    protected CspUserService cspUserService;

    @Autowired
    protected OauthService oauthService;

    @Autowired
    protected SysNotifyService sysNotifyService;

    @Autowired
    protected EmailTempService tempService;


    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Integer thirdPartyId){
        if (thirdPartyId == null || thirdPartyId == 0) {
            return localeView("/login/login");
        } else if (thirdPartyId > BindInfo.Type.YaYa.getTypeId() ){
            return localeView("/login/login_"+thirdPartyId);
        } else {
            // 第三方登录
            String url = oauthService.jumpThirdPartyAuthorizePage(thirdPartyId);
            return "redirect:" + url;
        }
    }


    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(String username, String password, Integer thirdPartyId,
                        String mobile, String captcha, Model model){
        //默认为邮箱密码登录
        if (thirdPartyId == null) {
            thirdPartyId = BindInfo.Type.EMAIL.getTypeId();
        }

        if (thirdPartyId == BindInfo.Type.EMAIL.getTypeId()) {
            // 邮箱登录
            return loginByEmail(username, password, model);

        } else if (thirdPartyId == BindInfo.Type.MOBILE.getTypeId()) {
            // 手机登录
            return loginByMobile(mobile, captcha, model);

        }
        return "" ;
    }

    /**
     * 邮箱登录
     * @param username
     * @param password
     * @param model
     * @return
     */
    protected String loginByEmail(String username, String password, Model model){
        String errorForwardUrl = localeView("/login/login_" + BindInfo.Type.EMAIL.getTypeId());
        if (CheckUtils.isEmpty(username)) {
            model.addAttribute("error", local("user.empty.username"));
            model.addAttribute("username", username);
            return errorForwardUrl;
        }

        if (CheckUtils.isEmpty(password)) {
            model.addAttribute("error", local("user.empty.password"));
            model.addAttribute("username", username);
            return errorForwardUrl;
        }
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            return errorForwardUrl;
        }
        return "redirect:/mgr/meet/list";
    }

    /**
     * 手机登录
     * @param mobile
     * @param captcha
     * @param model
     * @return
     */
    protected String loginByMobile(String mobile, String captcha, Model model)   {
        String errorForwardUrl = localeView("/login/login_" + BindInfo.Type.MOBILE.getTypeId());
        if (StringUtils.isEmpty(mobile)) {
            model.addAttribute("error", local("user.empty.mobile"));
            model.addAttribute("mobile", mobile);
            return errorForwardUrl;
        }

        if (StringUtils.isEmpty(captcha)) {
            model.addAttribute("error", local("user.empty.captcha"));
            model.addAttribute("captcha", captcha);
            return errorForwardUrl;
        }

        try {
            // 先登录成功之后再检查验证码 是否有效（避免如果登录出现异常 浪费短信验证码）
            UsernamePasswordToken token = new UsernamePasswordToken(mobile, "");
            token.setHost("mobile");
            Subject subject = SecurityUtils.getSubject();
            subject.login(token);

            // 检查验证码是否有效
            checkCaptchaIsOrNotValid(mobile, captcha);

        } catch (AuthenticationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("mobile", mobile);
            return errorForwardUrl;
        } catch (SystemException ex) {
           // 登录异常，需退出登录
            Subject subject = SecurityUtils.getSubject();
            subject.logout();

            model.addAttribute("error", ex.getMessage());
            model.addAttribute("mobile", mobile);
            return errorForwardUrl;
        }
        return "redirect:/mgr/meet/list";
    }



    /**
     * 第三方登录,绑定 回调地址
     * @param code
     * @param thirdPartyId
     * @return
     */
    @RequestMapping(value = "/oauth/callback")
    public String callback(String code, Integer thirdPartyId, RedirectAttributes redirectAttributes) throws SystemException {
        Principal principal =  getWebPrincipal();
        if (StringUtils.isNotEmpty(code)) {
            //获取第三方用户信息
            OAuthUser oAuthUser = oauthService.getOauthUser(code, thirdPartyId);

            if (oAuthUser != null) {
                String uniqueId = oAuthUser.getUid();
                if (StringUtils.isNotEmpty(uniqueId)) {

                    // 根据第三方用户唯一id 查询用户是否存在
                    CspUserInfo userInfo = cspUserService.findBindUserByUniqueId(uniqueId);

                    if(principal != null){
                        //第三方绑定操作
                        doThirdPartWebBind(oAuthUser,thirdPartyId,redirectAttributes);
                        return "redirect:/mgr/user/toAccount";
                    }else{
                        //第三方登录操作
                        doThirdPartWebLogin(thirdPartyId, oAuthUser, userInfo);
                        return "redirect:/mgr/meet/list";
                    }
                }
            }
        }

        return "";

    }

    /**
     * 推特回调
     * @param str
     * @return
     * @throws SystemException
     */
    @RequestMapping("/twitterCallback")
    public String twitterCallback(String str) throws SystemException {

        if(StringUtils.isEmpty(str)){
            return "";
        }

        Principal principal =  getWebPrincipal();
        //登录回调
        if(principal == null){
            //将json转为OauthUser对象
            String uid = (String)JsonUtils.getValue(str,"id_str");
            OAuthUser oAuthUser = new OAuthUser();
            oAuthUser.setIconUrl((String)JsonUtils.getValue(str,"profile_image_url"));
            oAuthUser.setNickname((String)JsonUtils.getValue(str,"name"));
            oAuthUser.setUid(uid);
            // 根据第三方用户唯一id 查询用户是否存在
            CspUserInfo userInfo = cspUserService.findBindUserByUniqueId(uid);
            doThirdPartWebLogin(BindInfo.Type.TWITTER.getTypeId(), oAuthUser, userInfo);
            return "redirect:/mgr/meet/list";
        }else{  //绑定回调

            //将json转为BindInfo对象
            BindInfo info = new BindInfo();
            info.setThirdPartyId(BindInfo.Type.TWITTER.getTypeId());
            info.setUniqueId((String)JsonUtils.getValue(str,"id_str"));
            info.setId(StringUtils.nowStr());
            info.setBindDate(new Date());
            info.setAvatar((String)JsonUtils.getValue(str,"profile_image_url"));
            info.setNickName((String)JsonUtils.getValue(str,"name"));
            cspUserService.doBindThirdAccount(info,principal.getId());
            return "redirect:/mgr/user/toAccount";
        }
    }

    /**
     * 绑定第三方账号
     * @param oAuthUser
     * @param thirdPartId
     * @param redirectAttributes
     * @return
     */
    private void doThirdPartWebBind(OAuthUser oAuthUser,Integer thirdPartId,RedirectAttributes redirectAttributes) {
            String userId = getWebPrincipal().getId();
            BindInfo info = OAuthUser.buildToBindInfo(oAuthUser);
            info.setThirdPartyId(thirdPartId);
            info.setId(StringUtils.nowStr());

        try {
            cspUserService.doBindThirdAccount(info,userId);
            addFlashMessage(redirectAttributes,"绑定成功");
        } catch (SystemException e) {
           addFlashMessage(redirectAttributes,e.getMessage());
        }


    }


    /**
     * 第三方登录需要做的操作
     * @param thirdPartyId
     * @param oAuthUser
     * @param userInfo
     */
    private void doThirdPartWebLogin(Integer thirdPartyId, OAuthUser oAuthUser, CspUserInfo userInfo) {
        // 用户不存在，去添加绑定账号及添加csp用户账号
        if (userInfo == null) {
            CspUserInfoDTO dto = OAuthUser.buildToCspUserInfoDTO(oAuthUser);
            dto.setThirdPartyId(thirdPartyId);
            //去添加绑定账号
            userInfo = cspUserService.saveThirdPartyUserInfo(dto);
        }

        UsernamePasswordToken token = new UsernamePasswordToken();
        token.setHost("thirdParty");
        token.setUsername(userInfo.getId());
        Subject subject = SecurityUtils.getSubject();
        subject.login(token);
    }


    /**
     * 跳转注册页面
     * @return
     */
    @RequestMapping(value = "/to/register")
    public String toRegister() {
        return localeView("/register/register");
    }

    /**
     * 邮箱注册
     * @param userInfo
     * @return
     */
    @RequestMapping(value = "/register")
    @ResponseBody
    public String register(CspUserInfo userInfo) {
        EmailTemplate template = tempService.getTemplate(LocalUtils.getLocalStr(),EmailTemplate.Type.REGISTER.getLabelId());
        try {
            return cspUserService.register(userInfo,template);
        } catch (SystemException e) {
            return error(local(e.getMessage()));
        }
    }

    /**
     * 忘记密码
     * @return
     */
    @RequestMapping(value = "/to/reset/password")
    public String toResetPassWd() {
        return localeView("/register/to_reset_passwd");
    }

}