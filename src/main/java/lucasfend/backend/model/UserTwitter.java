package lucasfend.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Table(name= "twitterLogs")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserTwitter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String twitterId;
    private String username;
    private String profileImageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "followed_accounts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "account")
    private List<String> followedAccounts;
}
