package splitwise.service;

import splitwise.enums.SplitType;
import splitwise.model.Group;
import splitwise.model.User;
import splitwise.repository.GroupRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// The only service the client talks to. It owns no business rules itself; it looks up the
// group and hands the work to the right collaborator.
public class GroupService {

    private final GroupRepository repo;
    private final ExpenseService expenseService;
    private final DebtSimplificationService simplifier;

    public GroupService(GroupRepository repo, ExpenseService expenseService, DebtSimplificationService simplifier) {
        this.repo = repo;
        this.expenseService = expenseService;
        this.simplifier = simplifier;
    }

    public String createGroup(String name, List<User> members) {
        String id = UUID.randomUUID().toString();
        Group g = new Group(id, name);
        members.forEach(g::addMember);

        repo.save(g);
        return id;
    }

    public void addMember(String groupId, User user) {
        get(groupId).addMember(user);
    }

    public void addExpense(String groupId,
                           String description, double amount,
                           User paidBy, List<User> participants,
                           SplitType splitType, Map<User, Double> meta) {

        expenseService.addExpense(get(groupId), description, amount,
                paidBy, participants, splitType, meta);
    }

    public void simplifyDebts(String groupId) {
        simplifier.simplifyDebts(get(groupId));
    }

    public void printBalances(String groupId) {
        Group g = get(groupId);
        System.out.println("[" + g.getName() + "]");
        g.getMembers().forEach(u -> g.getBalanceSheet(u).print(u));
    }

    private Group get(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
    }
}
