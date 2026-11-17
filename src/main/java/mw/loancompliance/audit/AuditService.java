package mw.loancompliance.audit;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.stereotype.Service;
@Service public class AuditService {
    private final AuditRepository audit;
    private final UserRepository users;
    public AuditService(AuditRepository a,UserRepository u) {
        audit=a;
        users=u;
    }
    public void record(String email,String action,String type,Long id,String description) {
        var user=email==null?null:users.findByEmailIgnoreCase(email).orElse(null);
        audit.save(AuditRecord.event(user==null?null:user.getId(),user==null?null:user.getRole(),action,type,id,description));
    }
}
