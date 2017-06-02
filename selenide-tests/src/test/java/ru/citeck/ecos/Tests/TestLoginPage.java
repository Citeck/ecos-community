package ru.citeck.ecos.Tests;
import ru.citeck.ecos.pages.HomePage;
import org.junit.Assert;
import org.junit.Test;

public class TestLoginPage extends SelenideTests{
    @Test
    public void successLogin()
    {
        HomePage homePage = new HomePage();
        Assert.assertTrue("Alfresco » Домашняя страница".equals(homePage.getTitle()) || "Alfresco » User Dashboard".equals(homePage.getTitle()));
    }
}