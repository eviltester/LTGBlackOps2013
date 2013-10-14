package com.eviltester.redmine;


import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.Role;
import com.taskadapter.redmineapi.bean.User;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public class RedMineGUI {

    private final String redmineURL;
    private final WebDriverWait wait;
    WebDriver aDriver;

    /***************************************************
     * Didn't start with Page Objects, I started with high level
     * GUI actions. If I re-use these or actually created tests
     * then I'd refactor this stuff into page objects - still
     * called by the high level abstraction layer, but for
     * long term maintenance I'd want the maintenance granularity
     * that page objects provide
     ***************************************************/

    public RedMineGUI(String redmineUrl) {

        this.redmineURL = redmineUrl;

        /*
            I YAGNI'd and added GhostDriver into the mix but didn't need it.
            I've subsequently deleted the tools folder, but svn now has some
            additional binaries in it that it didn't need.
         */
        aDriver = new FirefoxDriver();
        aDriver.get(redmineUrl);

        wait = new WebDriverWait(aDriver, 10);
    }


    public void loginAs(String admin, String password) {

        aDriver.get(this.redmineURL + "/login");

        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#login-form  input[type='submit']")));

        aDriver.findElement(By.id("username")).sendKeys(admin);
        aDriver.findElement(By.id("password")).sendKeys(password);

        loginButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#loggedas a")));
    }

    public void quit(){
        aDriver.quit();
    }

    public void allocateUserToProjectAndRoles(User aUser, Project aProject, List<Role> roles) {

        String urlForProjectMembership = redmineURL + String.format("/users/%d/edit?tab=memberships", aUser.getId());

        aDriver.get(urlForProjectMembership);

        // div#tab-content-memberships form input[type='submit']
        WebElement addButton = wait.until(
                            ExpectedConditions.elementToBeClickable(By.cssSelector(
                                    "div#tab-content-memberships form input[type='submit']")));

        Select projects = new Select(aDriver.findElement(By.id("membership_project_id")));
        projects.selectByValue(String.valueOf(aProject.getId()));

        for(Role aRole : roles){
            WebElement roleCheckBox = aDriver.findElement(
                                            By.cssSelector("input#membership_role_ids_[value='" + aRole.getId() + "']"));

            if(!roleCheckBox.isSelected()){
                roleCheckBox.click();
            }
        }

        addButton.click();

        // this might be brittle and might have to wait for AJAX action to complete
        // probably OK because the server request will complete, we just may not
        // hang around for the response before the page reloads with more test actions
        // this is a potential source of tech debt


    }
}
