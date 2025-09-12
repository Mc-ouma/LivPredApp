fun testGGCombinations() {
    // Test GG and Over 2.5 combinations
    
    // Case 1: Both teams score (2-2) and total > 2.5 → WIN
    val result1 = OutcomeCalculator.calculateOutcome("GG and Over 2.5", "2-2", null)
    println("GG and Over 2.5 with score 2-2: $result1") // Should be "win"
    
    // Case 2: Both teams score (1-1) but total not > 2.5 → LOSE
    val result2 = OutcomeCalculator.calculateOutcome("GG and Over 2.5", "1-1", null)
    println("GG and Over 2.5 with score 1-1: $result2") // Should be "lose"
    
    // Case 3: Not both teams score (2-0) even if total > 2.5 → LOSE
    val result3 = OutcomeCalculator.calculateOutcome("GG and Over 2.5", "2-0", null)
    println("GG and Over 2.5 with score 2-0: $result3") // Should be "lose"
    
    // Test BTTS & Over 1.5 combinations
    
    // Case 4: Both teams score (1-1) and total > 1.5 → WIN
    val result4 = OutcomeCalculator.calculateOutcome("BTTS & Over 1.5", "1-1", null)
    println("BTTS & Over 1.5 with score 1-1: $result4") // Should be "win"
    
    // Case 5: Not both teams score (3-0) even if total > 1.5 → LOSE
    val result5 = OutcomeCalculator.calculateOutcome("BTTS & Over 1.5", "3-0", null)
    println("BTTS & Over 1.5 with score 3-0: $result5") // Should be "lose"
    
    // Test GG-NO and Under 2.5 combinations
    
    // Case 6: Not both teams score (2-0) and total < 2.5 → WIN
    val result6 = OutcomeCalculator.calculateOutcome("GG-NO and Under 2.5", "2-0", null)
    println("GG-NO and Under 2.5 with score 2-0: $result6") // Should be "win"
    
    // Case 7: Both teams score (1-1) even if total < 2.5 → LOSE
    val result7 = OutcomeCalculator.calculateOutcome("GG-NO and Under 2.5", "1-1", null)
    println("GG-NO and Under 2.5 with score 1-1: $result7") // Should be "lose"
}
