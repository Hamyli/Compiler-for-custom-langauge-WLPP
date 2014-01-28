import java.util.*;
import java.util.Random;

public class WLPPGen {
    Scanner in = new Scanner(System.in);

    // The set of terminal symbols in the WLPP grammar.
    Set<String> terminals = new HashSet<String>(Arrays.asList("BOF", "BECOMES",
         "COMMA", "ELSE", "EOF", "EQ", "GE", "GT", "ID", "IF", "INT", "LBRACE",
         "LE", "LPAREN", "LT", "MINUS", "NE", "NUM", "PCT", "PLUS", "PRINTLN",
         "RBRACE", "RETURN", "RPAREN", "SEMI", "SLASH", "STAR", "WAIN", "WHILE",
         "AMP", "LBRACK", "RBRACK", "NEW", "DELETE", "NULL"));

    ArrayList<String[]> symbols= new ArrayList<String[]>();

    // Data structure for storing the parse tree.
    public class Tree {
        List<String> rule;
        String type = "";

        ArrayList<Tree> children = new ArrayList<Tree>();

        // Does this node's rule match otherRule?
        boolean matches(String otherRule) {
            return tokenize(otherRule).equals(rule);
        }
    }

    // Divide a string into a list of tokens.
    List<String> tokenize(String line) {
        List<String> ret = new ArrayList<String>();
        Scanner sc = new Scanner(line);
        while (sc.hasNext()) {
            ret.add(sc.next());
        }
        return ret;
    }

    // Read and return wlppi parse tree
    Tree readParse(String lhs) {
        String line = in.nextLine();
        List<String> tokens = tokenize(line);
        Tree ret = new Tree();
        ret.rule = tokens;
        if (!terminals.contains(lhs)) {
            Scanner sc = new Scanner(line);
            sc.next(); // discard lhs
            while (sc.hasNext()) {
                String s = sc.next();
                ret.children.add(readParse(s));
            }
        }
        return ret;
    }

/*=========================================================================*/

    // Compute symbols defined in t
    void genSymbols(Tree t) {
      for(int i = 0; i<t.children.size(); i++){
        //base case when dcl is reached
        if(t.rule.get(0).equals("dcl")){
          //when type is INT
          if(t.children.get(0).children.size()==1){
            String symbol[] = {"int", t.children.get(1).rule.get(1)};
            for(int j = 0; j<symbols.size(); j++){
              if(symbol[1].equals(symbols.get(j)[1]))
                bail("already declared");
            }
            symbols.add(symbol);
          }
          //when type is INT STAR
          else if(t.children.get(0).children.size()==2){
            String symbol[] = {"int*", t.children.get(1).rule.get(1)};
            for(int j = 0; j<symbols.size(); j++){
              if(symbol[1].equals(symbols.get(j)[1]))
                bail("already declared");
            }
            symbols.add(symbol);
          }
          break;
        }
        //when factor is reached
        else if(t.rule.get(0).equals("factor") && t.rule.get(1).equals("ID")){
          String varName = t.children.get(0).rule.get(1);
          //System.out.println(varName);
          boolean flag = false;
          for(int j = 0; j<symbols.size(); j++){
            if(varName.equals(symbols.get(j)[1]))
              flag = true;
          }
          if(flag==false)
            bail("variable not declared");
          break;
        }
        //when everything else is reached
        else{
          genSymbols(t.children.get(i));
        }
      }
    }

    //prints the symbol table in proper format
    void printSymbols(){
      for(int i = 0; i<symbols.size(); i++){
        System.err.println(symbols.get(i)[1] + " " + symbols.get(i)[0]);
      }
    }

/*==========================================================================*/

    //find the keywords subtrees
    void findKeyword(Tree t){
      for(int i = 0; i<t.children.size(); i++){
        if(t.children.get(i).rule.get(0).equals("dcl") && i==5){
          String dclType = buildType(t.children.get(i).children.get(1));
          if(dclType.equals("int*"))
            bail("2nd parameter must be of type int");
        }
        else if(t.children.get(i).rule.get(0).equals("dcls")){
          buildDcls(t.children.get(i));
        }
        else if(t.children.get(i).rule.get(0).equals("statements")){
          buildStatement(t.children.get(i));
        }
        else if(t.children.get(i).rule.get(0).equals("expr")){
          String exprType = buildType(t.children.get(i));
          if(exprType.equals("int*")){
            bail("return type must be an int");
          }
        }
      }
    }

 //process the dcls
 void buildDcls(Tree t){
   //System.err.println(t.children.get(4).rule.get(0));
   if(t.children.size()==0){
     return;
   }
   else if(t.children.size()==5 && t.children.get(3).rule.get(0).equals("NUM")){
     String dclType = buildType(t.children.get(1).children.get(1));
     if(dclType.equals("int*"))
       bail("dcl becomes num must be of type int");
   }
   else if(t.children.size()==5 && t.children.get(3).rule.get(0).equals("NULL")){
     String dclType = buildType(t.children.get(1).children.get(1));
     if(dclType.equals("int"))
       bail("dcl becomes NULL must be of type int*");
   }
   return;
 }

 //process the statements
 void buildStatement(Tree t){
   if(t.children.size()==0){
     return;
   }
   else if(t.children.get(0).rule.get(0).equals("statements")){
     buildStatement(t.children.get(0));
     buildStatement(t.children.get(1));
   }
   else if(t.children.get(0).rule.get(0).equals("lvalue")){
     String lvalueType = buildType(t.children.get(0));
     String exprType = buildType(t.children.get(2));
     if(!(lvalueType.equals(exprType)))
       bail("lvalue becomes expr semi, must be same type");
   }
   else if(t.children.get(0).rule.get(0).equals("IF")){
     String testType = buildType(t.children.get(2));
     buildStatement(t.children.get(5));
     buildStatement(t.children.get(9));
   }
   else if(t.children.get(0).rule.get(0).equals("WHILE")){
     String testType = buildType(t.children.get(2));
     buildStatement(t.children.get(5));
   }
   else if(t.children.get(0).rule.get(0).equals("PRINTLN")){
     String exprType = buildType(t.children.get(2));
     if(exprType.equals("int*"))
       bail("println must be int");
   }
   else if(t.children.get(0).rule.get(0).equals("DELETE")){
     String exprType = buildType(t.children.get(3));
     if(exprType.equals("int"))
       bail("delete must be int*");
   }
   return;
 }

    //build types for all nodes related to expr
    String buildType(Tree t){
      //base case when no more children is found
      if(t.children.size()==0){
        if(t.rule.get(0).equals("ID")){
          for(int j = 0; j<symbols.size(); j++){
            if(symbols.get(j)[1].equals(t.rule.get(1))){
              t.type = symbols.get(j)[0];
              return symbols.get(j)[0];
            }
          }
        }
        else if(t.rule.get(0).equals("NUM")){
          t.type = "int";
          return "int";
        }
        else if(t.rule.get(0).equals("NULL")){
          t.type = "int*";
          return "int*";
        }
      }
      //when one children is found. iteratation continues
      else if(t.children.size()==1){
        t.type = buildType(t.children.get(0));
        return t.type;
      }
      //when test is found
      else if(t.rule.get(0).equals("test") && t.children.size()==3){
        String exprType = buildType(t.children.get(0));
        String exprType2 = buildType(t.children.get(2));
        if(!(exprType.equals(exprType2)))
          bail("test, both expr must be the same");
      }
      //when AMP lvalue is found
      else if(t.children.get(0).rule.get(0).equals("AMP") && t.children.get(1).rule.get(0).equals("lvalue")){
        String lvalueType = buildType(t.children.get(1));
        if(lvalueType.equals("int*"))
          bail("factor -> AMP lvalue, lvalue is of type int*");
        t.type = "int*";
        return t.type;
      }
      //when STAR factor is found
      else if(t.children.get(0).rule.get(0).equals("STAR") && t.children.get(1).rule.get(0).equals("factor")){
        String factorType = buildType(t.children.get(1));
        if(factorType.equals("int"))
          bail("STAR factor, factor must be of type int*");
        t.type = "int";
        return t.type;
      }
      //when term is followed by star, slash and pct
      else if(t.rule.get(0).equals("term") && t.children.size()==3){
        String termType = buildType(t.children.get(0));
        String factorType = buildType(t.children.get(2));
        if(termType.equals("int*") || factorType.equals("int*"))
          bail("term -> term blah factor, both have to be int");
        t.type = "int";
        return t.type;
      }
      //when expr plus term is found
      else if(t.children.get(0).rule.get(0).equals("expr") && t.children.get(1).rule.get(0).equals("PLUS") && t.children.get(2).rule.get(0).equals("term")){
        String exprType = buildType(t.children.get(0));
        String termType = buildType(t.children.get(2));
        if(exprType.equals("int") && termType.equals("int"))
          t.type = "int";
        else if(exprType.equals("int*") && termType.equals("int*"))
          bail("int* plus int*");
        else if(exprType.equals("int*") || termType.equals("int*"))
          t.type = "int*";
        return t.type;
      }
      //when expr minus term is found
      else if(t.children.get(0).rule.get(0).equals("expr") && t.children.get(1).rule.get(0).equals("MINUS") && t.children.get(2).rule.get(0).equals("term")){
        String exprType = buildType(t.children.get(0));
        String termType = buildType(t.children.get(2));
        if(exprType.equals("int") && termType.equals("int"))
          t.type = "int";
        else if(exprType.equals("int*") && termType.equals("int"))
          t.type = "int*";
        else if(exprType.equals("int*") && termType.equals("int*"))
          t.type = "int";
        else
          bail("expr minus term type error");
        return t.type;
      }
      //when LPAREN expr RPAREN is found
      else if(t.children.get(0).rule.get(0).equals("LPAREN") && t.children.get(1).rule.get(0).equals("expr") && t.children.get(2).rule.get(0).equals("RPAREN")){
        t.type = buildType(t.children.get(1));
        return t.type;
      }
      //when LPAREN lvalue RPAREN is found
      else if(t.children.get(0).rule.get(0).equals("LPAREN") && t.children.get(1).rule.get(0).equals("lvalue") && t.children.get(2).rule.get(0).equals("RPAREN")){
        t.type = buildType(t.children.get(1));
        return t.type;
      }
      else if(t.children.size()==5){
        String exprType = buildType(t.children.get(3));
        if(exprType.equals("int*"))
          bail("new int (expr), expr must be an int");
        t.type = "int*";
        return t.type;
      }
      return t.type;
    }

    // Print an error message and exit the program.
    void bail(String msg) {
        System.err.println("ERROR: " + msg);
        System.exit(0);
    }

/*=======================================================================*/

    // Generate the code for the parse tree t.
    String genCode(Tree t) {
        return procedureCode(t.children.get(1));
    }

    // generate the code for load address
    String loadaddr(Tree t, int register){
      return "lis $" + register + "\n" + ".word V" + t.rule.get(1) + "\n";
    }

    // generate the code for the load word command
    String lw(int a, int b){
      return "lw $" + a + ", 0($" + b + ")\n";
    }

    // generate the code for the store word command
    String sw(int a, int b){
      return "sw $" + a + ", 0($" + b + ")\n";
    }

    //generate the code for push
    String push(int a){
      return "sw $" + a + ", -4($30)\n"
        + "lis $" + a + "\n"
        + ".word 4\n"
        + "sub $30, $30, $" + a + "\n";
    }

    //generate the code for pop
    String pop(int a){
      return "lis $" + a + "\n"
        + ".word 4\n"
        + "add $30, $30, $" + a + "\n"
        + "lw $" + a + ", -4($30)\n";
    }

    // generate code for the procedure node
    String procedureCode(Tree t){
      String ret = "";
      if(t.children.get(3).children.get(0).rule.size()==2)
        ret = ret + "add $2, $0, $0\n";
      ret = ret + push(31);
      ret = ret + "lis $29\n" + ".word init\n" + "jalr $29\n";
      ret = ret + pop(31);
      ret = ret + push(31);
      ret = ret + loadaddr(t.children.get(3).children.get(1), 3) + sw(1, 3);
      ret = ret + loadaddr(t.children.get(5).children.get(1), 3) + sw(2, 3);
      ret = ret + dclsCode(t.children.get(8));
      ret = ret + statementsCode(t.children.get(9));
      ret = ret + exprCode(t.children.get(11));
      ret = ret + pop(31);
      ret = ret + "jr $31\n";
      for(int i = 0; i<symbols.size(); i++){
        ret = ret + "V" + symbols.get(i)[1] + ":  .word 0\n";
      }
      ret = ret
        +"print:\n"
        +"sw $1, -4($30)\n"
        +"sw $2, -8($30)\n"
        +"sw $3, -12($30)\n"
        +"sw $4, -16($30)\n"
        +"sw $5, -20($30)\n"
        +"sw $6, -24($30)\n"
        +"sw $7, -28($30)\n"
        +"sw $8, -32($30)\n"
        +"sw $9, -36($30)\n"
        +"sw $10, -40($30)\n"
        +"lis $3\n"
        +".word -40\n"
        +"add $30, $30, $3\n"
        +"lis $3\n"
        +".word 0xffff000c\n"
        +"lis $4\n"
        +".word 10\n"
        +"lis $5\n"
        +".word 4\n"
        +"add $6, $1, $0\n"
        +"slt $7, $1, $0\n"
        +"beq $7, $0, IfDone\n"
        +"lis $8\n"
        +".word 0x0000002d\n"
        +"sw $8, 0($3)\n"
        +"sub $6, $0, $6\n"
        +"IfDone:\n"
        +"add $9, $30, $0 \n"
        +"Loop:\n"
        +"divu $6, $4\n"
        +"mfhi $10\n"
        +"sw $10, -4($9)\n"
        +"mflo $6\n"
        +"sub $9, $9, $5\n"
        +"slt $10, $0, $6\n"
        +"bne $10, $0, Loop\n"
        +"lis $7\n"
        +".word 48\n"
        +"Loop2:\n"
        +"lw $8, 0($9)\n"
        +"add $8, $8, $7\n"
        +"sw $8, 0($3)\n"
        +"add $9, $9, $5\n"
        +"bne $9, $30, Loop2\n"
        +"sw $4, 0($3)\n"
        +"lis $3\n"
        +".word 40\n"
        +"add $30, $30, $3\n"
        +"lw $1, -4($30)\n"
        +"lw $2, -8($30)\n"
        +"lw $3, -12($30)\n"
        +"lw $4, -16($30)\n"
        +"lw $5, -20($30)\n"
        +"lw $6, -24($30)\n"
        +"lw $7, -28($30)\n"
        +"lw $8, -32($30)\n"
        +"lw $9, -36($30)\n"
        +"lw $10, -40($30)\n"
        +"jr $31\n"
      + "init:\n"
+ "   sw $1, -4($30)\n"
+ "   sw $2, -8($30)\n"
+ "   sw $3, -12($30)\n"
+ "   sw $4, -16($30)\n"
+ "   sw $5, -20($30)\n"
+ "   sw $6, -24($30)\n"
+ "   sw $7, -28($30)\n"
+ "   sw $8, -32($30)\n"
+ "\n"
+ "   lis $4\n"
+ "   .word 32\n"
+ "   sub $30, $30, $4\n"
+ "\n"
+ "   lis $1\n"
+ "   .word end\n"
+ "   lis $3\n"
+ "   .word 1024       ; space for free list (way more than necessary)\n"
+ "\n"
+ "   lis $6\n"
+ "   .word 16         ; size of bookkeeping region at end of program\n"
+ "\n"
+ "   lis $7\n"
+ "   .word 4096       ; size of heap\n"
+ "\n"
+ "   lis $8\n"
+ "   .word 1\n"
+ "   add $2, $2, $2   ; Convert array length to words (*4)\n"
+ "   add $2, $2, $2\n"
+ "   add $2, $2, $6   ; Size of OS added by loader\n"
+ "\n"
+ "   add $5, $1, $6   ; end of program + length of bookkeeping\n"
+ "   add $5, $5, $2   ; + length of incoming array\n"
+ "   add $5, $5, $3   ; + length of free list\n"
+ "\n"
+ "   sw $5, 0($1)     ; store address of heap at Mem[end]\n"
+ "   add $5, $5, $7   ; store end of heap at Mem[end+4]\n"
+ "   sw $5, 4($1)\n"
+ "   sw $8, 8($1)     ; store initial size of free list (1) at Mem[end+8]\n"
+ "\n"
+ "   add $5, $1, $6\n"
+ "   add $5, $5, $2\n"
+ "   sw $5, 12($1)   ; store location of free list at Mem[end+12]\n"
+ "   sw $8, 0($5)    ; store initial contents of free list (1) at Mem[end+12]\n"
+ "   sw $0, 4($5)    ; zero-terminate the free list\n"
+ "\n"
+ "   add $30, $30, $4\n"
+ "\n"
+ "   lw $1, -4($30)\n"
+ "   lw $2, -8($30)\n"
+ "   lw $3, -12($30)\n"
+ "   lw $4, -16($30)\n"
+ "   lw $5, -20($30)\n"
+ "   lw $6, -24($30)\n"
+ "   lw $7, -28($30)\n"
+ "   lw $8, -32($30)\n"
+ "   jr $31\n"
+ "\n"
+ ";; new -- allocates memory (in 16-byte blocks)\n"
+ ";; $1 -- requested size in words\n"
+ ";; $3 -- address of allocated memory (0 if none available)  OUTPUT\n"
+ "new:\n"
+ "   sw $1, -4($30)\n"
+ "   sw $2, -8($30)\n"
+ "   sw $4, -12($30)\n"
+ "   sw $5, -16($30)\n"
+ "   sw $6, -20($30)\n"
+ "   sw $7, -24($30)\n"
+ "   sw $8, -28($30)\n"
+ "   sw $9, -32($30)\n"
+ "   sw $10, -36($30)\n"
+ "   sw $11, -40($30)\n"
+ "   sw $12, -44($30)\n"
+ "\n"
+ "   lis $10\n"
+ "   .word 44\n"
+ "   sub $30, $30, $10\n"
+ "\n"
+ "   ;; Make sure requested size > 0 ; if not, bail out.\n"
+ "   slt $3, $0, $1\n"
+ "   beq $3, $0, cleanupN\n"
+ "\n"
+ "   lis $11   ; $11 = 1\n"
+ "   .word 1\n"
+ "\n"
+ "   add $1, $1, $11 ; One extra word to store deallocation info\n"
+ "   add $1, $1, $1  ; Convert $1 from words to bytes\n"
+ "   add $1, $1, $1\n"
+ "\n"
+ "   add $2, $11, $11  ; $2 = 2\n"
+ "   add $4, $0, $0  ; $4 = counter, to accumulate ceil(log($1))\n"
+ "\n"
+ "   ;; Repeatedly dividing $1 by 2 and counting the divisions gives\n"
+ "   ;; floor (log($1)).  To get ceil(log($1)), evaluate floor(log($1-1))+1\n"
+ "   sub $1, $1, $11  ; So subtract 1 from $1\n"
+ "\n"
+ " topN:  ; Repeatedly divide $1 by 2, and count iterations\n"
+ "   beq $1, $0, endloopN\n"
+ "   div $1, $2      ; $1 /= 2\n"
+ "   mflo $1\n"
+ "   add $4, $4, $11  ; $4++\n"
+ "\n"
+ "   beq $0, $0, topN\n"
+ " endloopN:\n"
+ "\n"
+ "   add $1, $1, $11  ; Now add 1 to $1 to restore its value after previous sub\n"
+ "   add $4, $4, $11  ; And add 1 to $4 to complete ceil calculation (see above)\n"
+ "\n"
+ "   ;; An address' allocation code will consist of $14-$4 bits\n"
+ "   lis $5     ; $5 = 14\n"
+ "   .word 14  \n"
+ "\n"
+ "   sub $4, $5, $4  ; $4 <- 14 - $4  \n"
+ "\n"
+ "   ;; Cap the number of bits in an allocation code at 9 (so we don't allocate\n"
+ "   ;; blocks smaller than 4 words at a time).\n"
+ "   lis $5\n"
+ "   .word 9\n"
+ "\n"
+ "   slt $6, $5, $4 \n"
+ "   beq $6, $0, doNotFixN\n"
+ "   add $4, $5, $0\n"
+ "\n"
+ " doNotFixN:\n"
+ "   ; Make sure requested size is not too big, i.e., $4>0\n"
+ "   slt $3, $0, $4\n"
+ "   beq $3, $0, cleanupN\n"
+ "\n"
+ "   ; Now search for a word in the free list with that many bits or fewer\n"
+ "   ; (Fewer bits = larger block size)\n"
+ "   ; Compute largest possible $4-bit number, store in $7\n"
+ "   add $6, $4, $0    ; countdown from $4 to 0\n"
+ "   add $7, $11, $0   ; accumulates result by doubling $4 times\n"
+ " top2N:\n"
+ "   add $7, $7, $7    ; double $7\n"
+ "   sub $6, $6, $11   ; $6--\n"
+ "   bne $6, $0, top2N\n"
+ "\n"
+ "   sub $7, $7, $11  ; At the end of the loop, $7 = 2^$4 - 1\n"
+ "\n"
+ "   ; Find largest word in freelist <= $7\n"
+ "   lis $8\n"
+ "   .word findWord\n"
+ "   sw $31, -4($30)\n"
+ "   lis $31\n"
+ "   .word 4\n"
+ "   sub $30, $30, $31\n"
+ "   jalr $8          ; call findWord\n"
+ "   lis $31\n"
+ "   .word 4\n"
+ "   add $30, $30, $31\n"
+ "   lw $31, -4($30)\n"
+ "\n"
+ "   ; If no match found, cleanup and abort\n"
+ "\n"
+ "   beq $3, $0, cleanupN  ; if allocation fails, clean up and return 0\n"
+ "   \n"
+ "     ; Compute minimum code for exact match  (($7+1)/2)\n"
+ "   add $7, $7, $11\n"
+ "   div $7, $2\n"
+ "   mflo $7\n"
+ "   ; If exact match found, remove it from the free list\n"
+ " exactN:\n"
+ "   slt $6, $3, $7\n"
+ "   bne $6, $0, largerN\n"
+ "\n"
+ "   beq $0, $0, convertN\n"
+ "\n"
+ "   ; If larger match found, split into smaller buddies\n"
+ " largerN:  ;; buddies are 2$3 and 2$3+1\n"
+ "   add $3, $3, $3 ;; double $3\n"
+ "   ; add 2$3+1 to free list; evaluate 2$3 as possible candidate\n"
+ "   lis $6   ;; $6 = address of address of free list\n"
+ "   .word free\n"
+ "   lw $8, -4($6)  ;; $8 = length of free list\n"
+ "   lw $6, 0($6)   ;; $6 = address of free list\n"
+ "   add $8, $8, $8 ;; convert to words (*4)\n"
+ "   add $8, $8, $8\n"
+ "   add $6, $6, $8 ;; address of next spot in free list\n"
+ "   add $8, $3, $11 ;; $8 = buddy\n"
+ "   sw $8, 0($6)   ;; add to end of list\n"
+ "   sw $0, 4($6)\n"
+ "   ;; increment length of free list\n"
+ "   lis $6\n"
+ "   .word free\n"
+ "   lw $8, -4($6)\n"
+ "   add $8, $8, $11\n"
+ "   sw $8, -4($6)\n"
+ "\n"
+ "   ; now go back to exact with new value of $3, and re-evaluate\n"
+ "   beq $0, $0, exactN\n"
+ "\n"
+ "   ; Convert number to address\n"
+ " convertN:\n"
+ "   add $12, $3, $0  ; retain original freelist word\n"
+ "   add $7, $0, $0 ;; offset into heap\n"
+ "   lis $8\n"
+ "   .word end\n"
+ "   lw $9, 4($8)  ;; end of heap\n"
+ "   lw $8, 0($8)  ;; beginning of heap\n"
+ "   sub $9, $9, $8 ;; size of heap (bytes)\n"
+ " top5N:\n"
+ "   beq $3, $11, doneconvertN\n"
+ "   div $3, $2\n"
+ "   mflo $3    ;; $3/2\n"
+ "   mfhi $10   ;; $3%2\n"
+ "   beq $10, $0, evenN\n"
+ "   add $7, $7, $9   ;; add size of heap to offset\n"
+ " evenN:\n"
+ "   div $7, $2       ;; divide offset by 2\n"
+ "   mflo $7\n"
+ "   beq $0, $0, top5N\n"
+ "\n"
+ " doneconvertN:\n"
+ "   add $3, $8, $7  ;; add start of heap to offset to get address\n"
+ "   lis $4\n"
+ "   .word 4\n"
+ "   add $3, $3, $4  ;; advance one byte for deallocation info\n"
+ "   sw $12, -4($3)  ;; store deallocation info\n"
+ "\n"
+ " cleanupN:\n"
+ "   lis $10\n"
+ "   .word 44\n"
+ "   add $30, $30, $10\n"
+ "\n"
+ "   lw $1, -4($30)\n"
+ "   lw $2, -8($30)\n"
+ "   lw $4, -12($30)\n"
+ "   lw $5, -16($30)\n"
+ "   lw $6, -20($30)\n"
+ "   lw $7, -24($30)\n"
+ "   lw $8, -28($30)\n"
+ "   lw $9, -32($30)\n"
+ "   lw $10, -36($30)\n"
+ "   lw $11, -40($30)\n"
+ "   lw $12, -44($30)\n"
+ "   jr $31\n"
+ "\n"
+ ";; delete -- frees allocated memory\n"
+ ";; $1 -- address to be deleted\n"
+ "delete:\n"
+ "   sw $1, -4($30)\n"
+ "   sw $2, -8($30)\n"
+ "   sw $3, -12($30)\n"
+ "   sw $4, -16($30)\n"
+ "   sw $5, -20($30)\n"
+ "   sw $6, -24($30)\n"
+ "   sw $11, -28($30)\n"
+ "   sw $12, -32($30)\n"
+ "   sw $14, -36($30)\n"
+ "\n"
+ "   lis $6\n"
+ "   .word 36\n"
+ "   sub $30, $30, $6\n"
+ "\n"
+ "   lis $11\n"
+ "   .word 1\n"
+ "\n"
+ "   lis $12\n"
+ "   .word 2\n"
+ "\n"
+ "   lis $14\n"
+ "   .word 4\n"
+ "\n"
+ "   lw $2, -4($1) ;; buddy code for the allocated block\n"
+ "\n"
+ " nextBuddyD:\n"
+ "   beq $2, $11, notFoundD  ;; if there is no buddy (i.e. buddy code=1), bail out\n"
+ "   ;; compute buddy's buddy code  (i.e, add 1 if code is even, sub 1 if odd)\n"
+ "   add $3, $2, $0\n"
+ "   div $3, $12   ; $4 = $3 % 2\n"
+ "   mfhi $4\n"
+ "\n"
+ "   beq $4, $0, evenD\n"
+ "   sub $3, $3, $11\n"
+ "   beq $0, $0, doneParityD\n"
+ " evenD:\n"
+ "   add $3, $3, $11\n"
+ " doneParityD:\n"
+ "\n"
+ "   ;; Now search free list for the buddy; if found, remove, and divide the\n"
+ "   ;; buddy code by 2; if not found, add current buddy code to the free list.\n"
+ "   lis $5\n"
+ "   .word findAndRemove\n"
+ "   sw $31, -4($30)\n"
+ "   sub $30, $30, $14\n"
+ "   add $1, $3, $0\n"
+ "   jalr $5\n"
+ "   add $30, $30, $14\n"
+ "   lw $31, -4($30)\n"
+ "\n"
+ "   ;; If the procedure succeeded in finding the buddy, $3 will be 1; else it\n"
+ "   ;; will be 0.\n"
+ "   beq $3, $0, notFoundD\n"
+ "   div $2, $12\n"
+ "   mflo $2\n"
+ "   beq $0, $0, nextBuddyD\n"
+ "\n"
+ "  notFoundD:\n"
+ "   lis $4   ;; address of address of free list\n"
+ "   .word free\n"
+ "   lw $5, -4($4) ; length of the free list\n"
+ "   lw $4, 0($4)  ;; address of the free list\n"
+ "\n"
+ "   add $5, $5, $5  ; convert to offset\n"
+ "   add $5, $5, $5\n"
+ "   add $5, $4, $5  ; address of next spot in free list\n"
+ "   sw $2, 0($5)    ; put code back into free list\n"
+ "   sw $0, 4($5)    ; keep free list 0-terminated\n"
+ "\n"
+ "   ; update size of free list\n"
+ "   lis $4\n"
+ "   .word free\n"
+ "   lw $5, -4($4)\n"
+ "   add $5, $5, $11\n"
+ "   sw $5, -4($4)\n"
+ "\n"
+ "   lis $6\n"
+ "   .word 36\n"
+ "   add $30, $30, $6\n"
+ "\n"
+ "   lw $1, -4($30)\n"
+ "   lw $2, -8($30)\n"
+ "   lw $3, -12($30)\n"
+ "   lw $4, -16($30)\n"
+ "   lw $5, -20($30)\n"
+ "   lw $6, -24($30)\n"
+ "   lw $11, -28($30)\n"
+ "   lw $12, -32($30)\n"
+ "   lw $14, -36($30)\n"
+ "   jr $31\n"
+ "\n"
+ ";; findWord -- find and remove largest word from free list <= given limit\n"
+ ";;             return 0 if not possible\n"
+ ";; Registers:\n"
+ ";;   $7 -- limit\n"
+ ";;   $3 -- output\n"
+ "findWord:\n"
+ "    sw $1, -4($30)\n"
+ "    sw $2, -8($30)\n"
+ "    sw $4, -12($30)\n"
+ "    sw $5, -16($30)\n"
+ "    sw $6, -20($30)\n"
+ "    sw $7, -24($30)\n"
+ "    sw $8, -28($30)\n"
+ "    sw $9, -32($30)\n"
+ "    sw $10, -36($30)\n"
+ "    lis $1\n"
+ "    .word 36\n"
+ "    sub $30, $30, $1\n"
+ "    \n"
+ "    ;; $1 = start of free list\n"
+ "    ;; $2 = length of free list\n"
+ "    lis $1  ;; address of address of the free list\n"
+ "    .word free\n"
+ "    lw $2, -4($1)\n"
+ "    lw $1, 0($1) ;; address of the free list\n"
+ "    lis $4   ; $4 = 4 (for looping increments over memory)\n"
+ "    .word 4\n"
+ "    lis $9   ; $9 = 1 (for loop decrements)\n"
+ "    .word 1\n"
+ "\n"
+ "    add $3, $0, $0  ;; initialize output to 0 (not found)\n"
+ "    add $10, $0, $0 ;; for address of max word\n"
+ "    beq $2, $0, cleanupFW  ;; skip if no free memory\n"
+ "    add $5, $2, $0  ;; loop countdown to 0\n"
+ " topFW:\n"
+ "    lw $6, 0($1)\n"
+ "    slt $8, $7, $6  ;; limit < current item (i.e. item ineligible?)\n"
+ "    bne $8, $0, ineligibleFW\n"
+ "    slt $8, $3, $6  ;; max < current item?\n"
+ "    beq $8, $0, ineligibleFW  ; if not, skip to ineligible\n"
+ "    add $3, $6, $0  ;; replace max with current\n"
+ "    add $10, $1, $0 ;; address of current\n"
+ " ineligibleFW:\n"
+ "    add $1, $1, $4  ;; increment address\n"
+ "    sub $5, $5, $9  ;; decrement loop counter\n"
+ "    bne $5, $0, topFW     ;; if items left, continue looping\n"
+ "\n"
+ " ;; if candidate not found, bail out (if not found, $3 will still be 0)\n"
+ "    beq $3, $0, cleanupFW\n"
+ "\n"
+ " ;; now loop from $10 to end, moving up array elements\n"
+ " top2FW:\n"
+ "    lw $6, 4($10)  ;; grab next element in array\n"
+ "    sw $6, 0($10)  ;; store in current position\n"
+ "    add $10, $10, $4 ;; increment address\n"
+ "    bne $6, $0, top2FW  ;; continue while elements nonzero\n"
+ "\n"
+ " ;; decrement length of free list\n"
+ "    lis $2\n"
+ "    .word end\n"
+ "    lw $4, 8($2)\n"
+ "    sub $4, $4, $9  ; $9 still 1\n"
+ "    sw $4, 8($2)\n"
+ "\n"
+ " cleanupFW:\n"
+ "\n"
+ "    lis $1\n"
+ "    .word 36\n"
+ "    add $30, $30, $1\n"
+ "    lw $1, -4($30)\n"
+ "    lw $2, -8($30)\n"
+ "    lw $4, -12($30)\n"
+ "    lw $5, -16($30)\n"
+ "    lw $6, -20($30)\n"
+ "    lw $7, -24($30)\n"
+ "    lw $8, -28($30)\n"
+ "    lw $9, -32($30)\n"
+ "    lw $10, -36($30)\n"
+ "    jr $31\n"
+ "\n"
+ ";; findAndRemove -- find and remove given word from free list\n"
+ ";;             return 1 for success, 0 for failure\n"
+ ";; Registers:\n"
+ ";;   $1 -- word to remove\n"
+ ";;   $3 -- output (1 = success, 0 = failure)\n"
+ "findAndRemove:\n"
+ "   sw $1, -4($30)\n"
+ "   sw $2, -8($30)\n"
+ "   sw $4, -12($30)\n"
+ "   sw $5, -16($30)\n"
+ "   sw $6, -20($30)\n"
+ "   sw $7, -24($30)\n"
+ "   sw $8, -28($30)\n"
+ "   sw $9, -32($30)\n"
+ "   sw $11, -36($30)\n"
+ "   sw $14, -40($30)\n"
+ "\n"
+ "   lis $9\n"
+ "   .word 40\n"
+ "   sub $30, $30, $9\n"
+ "\n"
+ "   lis $11\n"
+ "   .word 1\n"
+ "\n"
+ "   lis $14\n"
+ "   .word 4\n"
+ "\n"
+ "   lis $2     ;; address of address of the free list\n"
+ "   .word free\n"
+ "   lw $4, -4($2) ;; length of the free list\n"
+ "   lw $2, 0($2)  ;; address of the free list\n"
+ "\n"
+ "\n"
+ "   add $3, $0, $0 ; success code\n"
+ "   add $6, $0, $0 ; address of found code\n"
+ "   add $7, $0, $0 ; loop counter\n"
+ "\n"
+ " topFaR:  ; loop through free list, looking for the code\n"
+ "   beq $4, $0, cleanupFaR\n"
+ "   lw $5, 0($2) ; next code in list\n"
+ "   bne $5, $1, notEqualFaR  ;; compare with input\n"
+ "   add $6, $6, $2  ; if code found, save its address\n"
+ "   beq $0, $0, removeFaR\n"
+ "\n"
+ " notEqualFaR:  ; current item not the one we're looking for; update counters\n"
+ "   add $2, $2, $14\n"
+ "   add $7, $7, $11\n"
+ "   bne $7, $4, topFaR\n"
+ "\n"
+ " removeFaR:\n"
+ "   beq $6, $0, cleanupFaR  ;; if code not found, bail out\n"
+ "\n"
+ " top2FaR:  ; now loop through the rest of the free list, moving each item one\n"
+ "           ; slot up\n"
+ "   lw $8, 4($2)\n"
+ "   sw $8, 0($2)\n"
+ "   add $2, $2, $14  ; add 4 to current address\n"
+ "   add $7, $7, $11  ; add 1 to loop counter\n"
+ "   bne $7, $4, top2FaR\n"
+ "   add $3, $11, $0  ;; set success code\n"
+ "\n"
+ "   ;; decrement size\n"
+ "   lis $2\n"
+ "   .word free\n"
+ "   lw $5, -4($2)\n"
+ "   sub $5, $5, $11\n"
+ "   sw $5, -4($2)\n"
+ "\n"
+ " cleanupFaR:\n"
+ "   lis $9\n"
+ "   .word 40\n"
+ "   add $30, $30, $9\n"
+ "\n"
+ "   lw $1, -4($30)\n"
+ "   lw $2, -8($30)\n"
+ "   lw $4, -12($30)\n"
+ "   lw $5, -16($30)\n"
+ "   lw $6, -20($30)\n"
+ "   lw $7, -24($30)\n"
+ "   lw $8, -28($30)\n"
+ "   lw $9, -32($30)\n"
+ "   lw $11, -36($30)\n"
+ "   lw $14, -40($30)\n"
+ "   jr $31\n"
+ "\n"
+ ";; printFreeList -- prints the contents of the free list, for testing and\n"
+ ";;  debugging purposes.  Requires a print routine for $1 to be linked in.\n"
+ ";;  Registers:\n"
+ ";;    Input -- none\n"
+ ";;    Output -- none\n"
+ "printFreeList:\n"
+ "   sw $1, -4($30)\n"
+ "   sw $2, -8($30)\n"
+ "   sw $3, -12($30)\n"
+ "   sw $4, -16($30)\n"
+ "   sw $5, -20($30)\n"
+ "   sw $6, -24($30)\n"
+ "   sw $7, -28($30)\n"
+ "   sw $8, -32($30)\n"
+ "   lis $6\n"
+ "   .word 32\n"
+ "   sub $30, $30, $6\n"
+ "\n"
+ "   lis $3   ; address of address of the start of the free list\n"
+ "   .word free\n"
+ "   lis $4\n"
+ "   .word 4\n"
+ "   lis $5   ; external print procedure\n"
+ "   .word print\n"
+ "   lis $6\n"
+ "   .word 1\n"
+ "\n"
+ "   lw $2, -4($3) ; $2 = length of free list; countdown to 0 for looping\n"
+ "   lw $3, 0($3) ; $3 = address of the start of the free list\n"
+ "\n"
+ "   ;; loop through the free list, and print each element\n"
+ " topPFL:\n"
+ "   beq $2, $0, endPFL  ;; skip if free list empty\n"
+ "\n"
+ "   lw $1, 0($3)     ; store in $1 the item to be printed\n"
+ "   sw $31, -4($30)\n"
+ "   sub $30, $30, $4\n"
+ "   jalr $5          ; call external print procedure\n"
+ "   add $30, $30, $4\n"
+ "   lw $31, -4($30)\n"
+ "   add $3, $3, $4   ; update current address and loop counter\n"
+ "   sub $2, $2, $6\n"
+ "   bne $2, $0, topPFL\n"
+ "\n"
+ " endPFL:\n"
+ "   ;; add an extra newline at the end, so that if this procedure is called\n"
+ "   ;; multiple times, we can distinguish where one call ends and the next\n"
+ "   ;; begins\n"
+ "   lis $6\n"
+ "   .word 0xffff000c\n"
+ "   lis $5\n"
+ "   .word 10\n"
+ "   sw $5, 0($6)\n"
+ "\n"
+ "   lis $6\n"
+ "   .word 32\n"
+ "   add $30, $30, $6\n"
+ "   lw $1, -4($30)\n"
+ "   lw $2, -8($30)\n"
+ "   lw $3, -12($30)\n"
+ "   lw $4, -16($30)\n"
+ "   lw $5, -20($30)\n"
+ "   lw $6, -24($30)\n"
+ "   lw $7, -28($30)\n"
+ "   lw $8, -32($30)\n"
+ "   jr $31\n"
+ "end:\n"
+ "   .word 0 ;; beginnning of heap\n"
+ "   .word 0 ;; end of heap\n"
+ "   .word 0 ;; length of free list\n"
+ "free: .word 0 ;; beginning of free list\n";

      return ret;
    }

    //generate code for the term node
    String termCode(Tree t){
      if(t.children.size()==1)
        return factorCode(t.children.get(0));
      else if(t.children.size()==3 && t.children.get(1).rule.get(0).equals("STAR")){
        return termCode(t.children.get(0)) + push(3) + factorCode(t.children.get(2))
          + pop(1) + "mult $1, $3\n" + "mflo $3\n";
      }
      else if(t.children.size()==3 && t.children.get(1).rule.get(0).equals("SLASH")){
        return termCode(t.children.get(0)) + push(3) + factorCode(t.children.get(2))
          + pop(1) + "div $1, $3\n" + "mflo $3\n";
      }
      else if(t.children.size()==3 && t.children.get(1).rule.get(0).equals("PCT")){
        return termCode(t.children.get(0)) + push(3) + factorCode(t.children.get(2))
          + pop(1) + "div $1, $3\n" + "mfhi $3\n";
      }
      return "ERROR term";
    }

    //generate code for the factor node
    String factorCode(Tree t){
      if(t.children.get(0).rule.get(0).equals("ID"))
        return loadaddr(t.children.get(0), 1) + lw(3, 1);
      else if(t.children.get(0).rule.get(0).equals("NUM")){
        return "lis $3\n" + ".word " + t.children.get(0).rule.get(1) + "\n";
      }
      else if(t.children.size()==3 && t.children.get(1).rule.get(0).equals("expr"))
        return exprCode(t.children.get(1));
      else if(t.children.get(0).rule.get(0).equals("NULL")){
        return "lis $3\n" + ".word 1\n";
      }
      else if(t.children.get(0).rule.get(0).equals("AMP")){
        return addressCode(t.children.get(1));
      }
      else if(t.children.get(0).rule.get(0).equals("STAR")){
        return factorCode(t.children.get(1)) + lw(3, 3);
      }
      else if(t.children.get(0).rule.get(0).equals("NEW")){
        return exprCode(t.children.get(3)) + "add $1, $3, $0\n" + "lis $29\n"
          + ".word new\n" + "jalr $29\n";
      }
      return "ERROR factor";
    }

    //generate code for the expr node
    String exprCode(Tree t){
      if(t.children.size()==1)
        return termCode(t.children.get(0));
      else if(t.children.size()==3 && t.children.get(1).rule.get(0).equals("PLUS")){
        String type1 = t.children.get(0).type;
        String type2 = t.children.get(2).type;
        if(type1.equals("int") && type2.equals("int")){
          return exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2))
            + pop(1) + "add $3, $1, $3\n";
        }
        else if(type1.equals("int*") && type2.equals("int")){
          return exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2))
            + pop(1) + "lis $4\n.word 4\nmult $3, $4\nmflo $3\nadd $3, $1, $3\n";
        }
        else if(type1.equals("int") && type2.equals("int*")){
          return exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2))
            + pop(1) + "lis $4\n.word 4\nmult $1, $4\nmflo $1\nadd $3, $1, $3\n";
        }
      }
      else if(t.children.size()==3 && t.children.get(1).rule.get(0).equals("MINUS")){
        String type1 = t.children.get(0).type;
        String type2 = t.children.get(2).type;
        if(type1.equals("int") && type2.equals("int")){
          return exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2))
            + pop(1) + "sub $3, $1, $3\n";
        }
        else if(type1.equals("int*") && type2.equals("int")){
          return exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2))
            + pop(1) + "lis $4\n.word 4\nmult $3, $4\nmflo $3\nsub $3, $1, $3\n";
        }
        else if(type1.equals("int*") && type2.equals("int*")){
          return exprCode(t.children.get(0)) + push(3) + termCode(t.children.get(2))
            + pop(1) + "lis $4\n.word 4\n sub $3, $1, $3\ndiv $3, $4\nmflo $3\n";
        }
      }
      return "ERROR expr";
    }

    //generate code for the statements node
    String statementsCode(Tree t){
      if(t.children.size()==0)
        return "";
      else{
        return statementsCode(t.children.get(0)) + statementCode(t.children.get(1));
      }
    }

    //generate code for the statement node
    String statementCode(Tree t){
      if(t.children.get(0).rule.get(0).equals("PRINTLN")){
        return exprCode(t.children.get(2)) + "add $1, $3, $0\n" + "lis $29\n"
          + ".word print\n" + "jalr $29\n";
      }
      else if(t.children.get(0).rule.get(0).equals("lvalue")){
        return exprCode(t.children.get(2)) + push(3) + addressCode(t.children.get(0))
          + pop(1) + sw(1, 3);
      }
      else if(t.children.get(0).rule.get(0).equals("WHILE")){
        String newLabel = newLabel();
        String begin = "begin" + newLabel;
        String end = "end" + newLabel;

        return "L" + begin + ":\n" + testCode(t.children.get(2), end)
          + statementsCode(t.children.get(5))
          + "beq $0, $0, L" + begin + "\n" + "L" + end + ":\n";
      }
      else if(t.children.get(0).rule.get(0).equals("IF")){
        String newLabel = newLabel();
        String endIF = "endif" + newLabel;
        String elseL = "else" + newLabel;

        return testCode(t.children.get(2), elseL) + statementsCode(t.children.get(5))
          + "beq $0, $0, L" + endIF + "\n" + "L" + elseL + ":\n"
          + statementsCode(t.children.get(9)) + "L" + endIF + ":\n";
      }
      else if(t.children.get(0).rule.get(0).equals("DELETE")){
        return exprCode(t.children.get(3)) + "add $1, $3, $0\n" + "lis $29\n"
          + ".word delete\n" + "jalr $29\n";
      }
      return "ERROR statement";
    }

    //generate code for lvalue node
    String addressCode(Tree t){
      if(t.children.size()==1){
        return loadaddr(t.children.get(0), 3);
      }
      else if(t.children.size()==3){
        return addressCode(t.children.get(1));
      }
      else if(t.children.get(0).rule.get(0).equals("STAR")){
        return factorCode(t.children.get(1));
      }
      return "Error lvalue";
    }

    //generate code for dcls
    String dclsCode(Tree t){
      if(t.children.size()==0)
        return "";
      else if(t.children.size()==5 && t.children.get(3).rule.get(0).equals("NUM")){
        return dclsCode(t.children.get(0)) + loadaddr(t.children.get(1).children.get(1), 3)
          + "lis $1\n" + ".word " + t.children.get(3).rule.get(1)
          + "\n" + sw(1, 3);
      }
      else if(t.children.size()==5 && t.children.get(3).rule.get(0).equals("NULL")){
        return dclsCode(t.children.get(0)) + loadaddr(t.children.get(1).children.get(1), 3)
          + "lis $1\n" + ".word 1\n" + sw(1, 3);
      }
      return "ERROR dcls";
    }

    //generate code for test node
    String testCode(Tree t, String label){
      if(t.children.get(1).rule.get(0).equals("LT")){
        return exprCode(t.children.get(0)) + push(3)
          + exprCode(t.children.get(2)) + pop(1)
          + "slt $1, $1, $3\n" + "beq $1, $0, L" + label + "\n";
      }
      else if(t.children.get(1).rule.get(0).equals("EQ")){
        return exprCode(t.children.get(0)) + push(3)
          + exprCode(t.children.get(2)) + pop(1)
          + "bne $1, $3, L" + label + "\n";
      }
      else if(t.children.get(1).rule.get(0).equals("NE")){
        return exprCode(t.children.get(0)) + push(3)
          + exprCode(t.children.get(2)) + pop(1)
          + "beq $1, $3, L" + label + "\n";
      }
      else if(t.children.get(1).rule.get(0).equals("LE")){
        return exprCode(t.children.get(0)) + push(3)
          + exprCode(t.children.get(2)) + pop(1)
          + "slt $1, $3, $1\n" + "bne $1, $0, L" + label + "\n";
      }
      else if(t.children.get(1).rule.get(0).equals("GE")){
        return exprCode(t.children.get(0)) + push(3)
          + exprCode(t.children.get(2)) + pop(1)
          + "slt $1, $1, $3\n" + "bne $1, $0, L" + label + "\n";
      }
      else if(t.children.get(1).rule.get(0).equals("GT")){
        return exprCode(t.children.get(0)) + push(3)
          + exprCode(t.children.get(2)) + pop(1)
          + "slt $1, $3, $1\n" + "beq $1, $0, L" + label + "\n";
      }
      return "ERROR test";
    }

    int labelcount = 0;
    String newLabel(){
      labelcount++;
      return "loop" + labelcount;
    }
/*=======================================================================*/

    // Main program
    public static final void main(String args[]) {
        new WLPPGen().go();
    }

    public void go() {
        Tree parseTree = readParse("S");
        genSymbols(parseTree.children.get(1));
        findKeyword(parseTree.children.get(1));
        String code = genCode(parseTree);
        System.out.println(code);
    }
}

