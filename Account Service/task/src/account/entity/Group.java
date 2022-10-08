//package account.entity;
//
//import javax.persistence.*;
//import java.util.Set;
//
//@Entity
//@Table(name = "principle_groups")
//public class Group{
//
//    //removed getter and setter to save space
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(unique = true, nullable = false)
//    private String code;
//    private String name;
//
//    @ManyToMany(mappedBy = "userGroups")
//    private Set<User> users;
//}