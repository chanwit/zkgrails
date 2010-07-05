package zk

class User {

    String name
    String password

    String toString() { "${name}" }
    
    static constraints = {
        name(nullable: true)
        password(password: true)
    }

}