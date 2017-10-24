package cn.medcn.user.service;

import cn.medcn.common.excptions.SystemException;
import cn.medcn.common.service.BaseService;
import cn.medcn.user.dto.CspUserInfoDTO;
import cn.medcn.user.model.BindInfo;
import cn.medcn.user.model.CspUserInfo;
import org.jdom.JDOMException;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

/**
 * Created by Liuchangling on 2017/9/26.
 */
public interface CspUserService extends BaseService<CspUserInfo>{

    /**
     * 根据uniqueId 查询用户是否存在
     * @param uniqueId
     * @return
     */
    CspUserInfo findBindUserByUniqueId(String uniqueId);

    /**
     * 查询绑定的第三方平台列表
     * @param userId
     * @return
     */
    List<BindInfo> findBindListByUserId(String userId);

    /**
     * 根据邮箱或者手机号码检查csp账号 是否存在
     * @param username
     * @return
     */
    CspUserInfo findByLoginName(String username);

    /**
     * 注册用户
     * @param userInfo
     */
    String register(CspUserInfo userInfo) throws SystemException;

    /**
     * 发送手机验证码
     * @param mobile
     * @param type
     */
    String sendCaptcha(String mobile, Integer type) throws SystemException ;

    /**
     * 检查验证码是否有效
     * @param captcha
     * @param mobile
     */
    void checkCaptchaIsOrNotValid(String mobile, String captcha) throws SystemException;

    /**
     * 添加第三方平台用户及绑定用户信息
     * @param userDTO
     * @return
     */
    CspUserInfo saveThirdPartyUserInfo(CspUserInfoDTO userDTO);

    /**
     * 缓存信息和发送绑定或找回密码邮件
     * @param email
     * @param userId
     * @param template 模板
     */
    void sendMail(String email, String userId, Integer template) throws SystemException;

    /**
     * 绑定手机号
     * @param mobile
     * @param captcha
     * @param userId
     */
    void doBindMobile(String mobile, String captcha, String userId) throws SystemException;

    /**
     * 解绑邮箱或手机
     * @param type
     * @param userId
     * @return
     */
    void doUnbindEmailOrMobile(Integer type, String userId) throws SystemException;

    /**
     * 绑定第三方账号
     * @param info
     * @param userId
     * @return
     */
    void doBindThirdAccount(BindInfo info, String userId) throws SystemException;

    /**
     * 解绑第三方账号
     * @param info
     * @param userId
     * @return
     */
    void doUnbindThirdAccount(BindInfo info, String userId) throws SystemException;

    /**
     * 绑定邮箱
     * @param key
     * @param result
     */
    void doBindMail(String key, String result) throws SystemException;

    /**
     * 修改头像
     * @param file
     * @return 头像地址
     */
    String updateAvatar(MultipartFile file,String userId) throws SystemException;

    /**
     * 修改密码
     * @param userId
     * @param oldPwd
     * @param newPwd
     */
    void resetPwd(String userId, String oldPwd, String newPwd) throws SystemException;

    /**
     * cspweb端查找用户信息
     * @param userId
     * @return
     */
    CspUserInfoDTO findCSPUserInfo(String userId);
}
