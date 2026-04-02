package com.ruoyi.auth.form;

/**
 * User register payload.
 */
public class RegisterBody extends LoginBody
{
    /**
     * User nickname.
     */
    private String nickname;

    /**
     * Phone number.
     */
    private String phonenumber;

    /**
     * Email.
     */
    private String email;

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
