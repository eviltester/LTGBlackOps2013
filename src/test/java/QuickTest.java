import com.eviltester.redmine.RedMineGUI;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.*;
import org.junit.Test;

import java.util.List;
import java.util.Random;


// Great name - but I'm exploring so anything goes
public class QuickTest {

    // Yeah, I would eventually refactor this stuff out into
    // Properties or environment variables
    public static final String REDMINE_URL = "http://192.168.1.141";
    public static final String REDMINE_AUTH_KEY = "4b1aad044f790da0fc0e316851d18d22a36ef895";

    public static final int NUMBER_OF_USERS_TO_CREATE = 100;
    public static final int NUMBER_OF_ISSUES_TO_CREATE = 1000;

    // requires Administration \ Settings \ Enable Rest API , then go into the admin account and get the API access key

    // http://www.redmine.org/projects/redmine/wiki/Rest_api_with_java
    // https://github.com/taskadapter/redmine-java-api
    // http://www.redmine.org/projects/redmine/wiki/Rest_Memberships
    @Test
    public void connection() throws RedmineException {

        // This has been heavily commented, not because I always comment
        // but because it is used in a training session and I'm putting my
        // notes in here rather than a slide deck

        /*  Notes:
            Originally started with Just the API, and started exploring
            the API for that. Hit an issue with adding roles and projects
            to a user, so investigated using the GUI for that.

            When I started the GUI I immediately abstracted it into
            a high level object. Mainly to try and keep the test
            cleaner because I hadn't refactored the test during exploration.

            Yes the hardcoded username/password would be moved into properties.
         */
        //RedMineGUI gui = new RedMineGUI(REDMINE_URL);
        //gui.loginAs("admin", "password");


        /*******************************************************************
         * Initial API Exploration
         ******************************************************************/

        /* Yeah, I encountered a bug in my intToAlphaString code
           instead of writing a unit test - because I was in a hurry
           I just wrote some System.out calls which I could debug through

           If this code was intended to hang around then I'd refactor
           this nonsense into a decent set of unit tests.
           But, if I'm exploring. and I don't know if the code is
           throw away or not, I confess, this is sometimes how I work.
         */
        // System.out.println(intToAlphaString(15));
        // System.out.println(intToAlphaString(46));

        RedmineManager mgr = new RedmineManager(REDMINE_URL, REDMINE_AUTH_KEY);

        List<Project> projects = mgr.getProjects();

        /*
            Since I don't know if the API is working or not, I
            output some obvious information as I work through. I left it in
            as debug console output.
            It could be an assert, but I'm exploring the api using a test,
            rather than writing a test that will run and self check.
        */
        System.out.println("" + projects.size());


        /* I initially thought that GROUPS were important. They
           seemed important when I skimmed the API documentation.
           Turns out they are unimportant, but at least I used the
           api to 'do' something, rather than just interrogate.
         */
        // Can Create a GROUP
        Group aGroup = new Group();
        aGroup.setName("testGroup");

        /*
            Since I'm running the test over and over with hard coded data
            I'm going to get "Thing x already exists errors. So just skip them.
        */

        try{
            mgr.createGroup(aGroup);
        }catch(Exception e){
            // ignore any duplicate groups we don't care
            e.printStackTrace();
        }


        List<Group> groups = mgr.getGroups();
        System.out.println("" + groups.size());
        Project aProject = projects.get(0);


        /*******************************************************************
         * Create a User Exploration
         ******************************************************************/

        /*
            Users are a pretty fundamental component of the system
            so I target this area first - if I can't create a user, I can't
            really do anything of value so my priority list thinking is
                Create User
                Create Issue
                ... Other stuff I haven't thought about yet
         */

        /*
            I figure out, by looking at the GUI rather than the API
            That roles are important. At this point I'm thinking that
            I have to create them using memberships
         */

        List<Role> roles = mgr.getRoles();
        List<Membership> memberships = mgr.getMemberships(aProject);

        // again - just checking that the API works
        // I actually breakpointed this line and looked at the list of roles and memberships in the
        // debugger to check the info and objects returned
        System.out.println("" + memberships.size());

        /*
         Initially I thought the API was broken and I couldn't add permissions to users
         But I figured out that I have to create the user (with not full data) then
         use the API to bring back the full user details
         Then use the full user object in future API calls
         Obvious really. Hence the note to self comment below
        */
        // Can allocate users to projects or memberships , but only if you refresh the user first!


        /*
            Yeah, I'm writing a lot of code in this test, I didn't take the time to refactor it out.
            That would come, and almost did happen because I was starting to get confused by the
            length of the method, but I toughed it out because it was a short term exploration and
            I'm that kind of guy
         */
        List<User> users;
        User aUser;
        User aNewUser;

        // get all the users to work out a 'unique' name, create the user, then 'refresh' the user
        // before allocating it roles
        for(int usersToCreateCount = 0; usersToCreateCount  < NUMBER_OF_USERS_TO_CREATE; usersToCreateCount ++){

            System.out.println( "Creating user " + usersToCreateCount);

            users = mgr.getUsers();

            aUser = new User();

            aUser.setLogin("user" + users.size());
            aUser.setFirstName("bob" + intToAlphaString(users.size()));
            aUser.setLastName("dobbs");
            aUser.setMail("bob.dobbs.132491023481243" + users.size() + "@mailinator.com");
            aUser.setPassword("password");
            //aUser.setMemberships(memberships);

            try{
                mgr.createUser(aUser);
            }catch(Exception e){
                // ignore duplicate users
                e.printStackTrace();
            }

            // get the full user details
            aNewUser = findUserByLogin(mgr.getUsers(), aUser.getLogin());
            //Integer userId = aNewUser.getId();



            // This membership creation only seems to work through the API if userID is set

            Membership aMembership = new Membership();
            // Added some randomisation to make test data spread out a bit
            aMembership.setProject(randomValueFrom(projects));
            // just add all the roles TODO: randomly pick a subset of roles
            aMembership.setRoles(roles);
            aMembership.setUser(aNewUser);

            try{
                mgr.addMembership(aMembership);
            }catch(Exception e){
                // ignore duplicate memberships
                e.printStackTrace();
            }
        }

        /*
            When the above API code was failing, I wrote the method below
            to allocate the details via the GUI.
            The code was quick to write, and helped me work out that I
            probably just needed to refresh the API object.

            So after I had it working through the GUI I went back above
            and fixed the code so it worked through the API.
         */
        // or allocate through the GUI
        // gui.allocateUserToProjectAndRoles(aNewUser, aProject, roles);


        /************************************************************************
         * Create an Issue exploration
         **************************************************************************/

        /*
            This worked pretty smoothly, initially I couldnt' allocate the issue to a user
            but that started working when I fixed the user creation code above.

            It worked fine when the user was allocated permissions through the GUI
            and that helped me work out what was wrong with my API usage.
         */

        // refresh the users list
        users = mgr.getUsers();

        /*
            I'm creating data so I'm just going to randomly allocate
            a status, an issue type, a priority etc.
         */
        List<IssueStatus> statuses = mgr.getStatuses();
        List<Tracker> trackers = mgr.getTrackers();
        List<IssuePriority> priorities = mgr.getIssuePriorities();

        // Can add Issues, assignee user must have an id

        for(int issueCount = 0; issueCount < NUMBER_OF_ISSUES_TO_CREATE; issueCount ++){

            System.out.println( "Creating issue " + issueCount);

            Issue anIssue = new Issue();
            anIssue.setProject(randomValueFrom(projects));
            anIssue.setAssignee(randomValueFrom(users));
            // TODO: create better random subject
            anIssue.setSubject("this subject is not blank " + issueCount);
            // TODO: create random description
            anIssue.setDescription("blah blah blah");

            anIssue.setTracker(randomValueFrom(trackers));
            anIssue.setPriorityId(randomValueFrom(priorities).getId());
            anIssue.setStatusId(randomValueFrom(statuses).getId());


            // TODO: expand to create sub tasks, note to self below
            // to create sub tasks and sub issues
            // anIssue.setParentId();

            mgr.createIssue(aProject.getIdentifier(), anIssue);
        }


        /**********************************************************************
         * What next after that? Time Entries look important so I'd look into
         * that next
         **********************************************************************/

        // TODO: create time entries
        //
        //TimeEntry aTimeEntry = new TimeEntry();
        //aTimeEntry.set
        //mgr.createTimeEntry();

        //gui.quit();

    }


    // I hacked this out, it was buggy, but I went debugging rather than TDD
    // In this case I don't think that slowed me down too much, but had I done
    // TDD it would probably have taken about the same time, but I would have
    // already done the work to create the tests, that I now have as a to do
    private String intToAlphaString(int theInt) {

        String intAsString = String.valueOf(theInt);

        String alphas = "abcdefghijklmnopqrstuvwyzy";

        String alphaString= "";

        for(int pos = 0; pos < intAsString.length(); pos++){
            String numberToConvert = Character.toString(intAsString.charAt(pos));
            int positionInAlphas =  Integer.valueOf(numberToConvert);
            alphaString += alphas.charAt(positionInAlphas);
        }

        return alphaString;
    }

    User findUserByLogin(List<User> users, String login){

        for(User aUser : users){
            if(aUser.getLogin().equals(login)){
                return aUser;
            }
        }
        return null;
    }

    // given a 'list' return a random object from that list
    public <T> T randomValueFrom(List <T>theList){
        Random rnd = new Random();
        return theList.get(rnd.nextInt(theList.size()));
    }
}
